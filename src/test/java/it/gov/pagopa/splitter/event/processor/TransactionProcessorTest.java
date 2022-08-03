package it.gov.pagopa.splitter.event.processor;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.splitter.BaseIntegrationTest;
import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import it.gov.pagopa.splitter.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


@Slf4j
@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.splitter.event.producer.TransactionRejectedProducer=WARN",
})
class TransactionProcessorTest extends BaseIntegrationTest {
    private final int hpanInitiativeNumber = 5;

    @Test
    void trxProcessor() {
        int transactionInputNumber = 101;
        long maxWaitingMs=30000;

        setInitiativeHpanForIncomingTransactions();

        //region send a TransactionsDTO
        long timePublishTransactionsStart=System.currentTimeMillis();
        IntStream.range(0, transactionInputNumber)
                .mapToObj(n->TransactionDTOFaker.mockInstanceWithNHpan(n,hpanInitiativeNumber))
                .forEach( t -> publishIntoEmbeddedKafka(topicTransactionInput,null, null,t));
        //endregion

        Consumer<String, String> consumer = getEmbeddedKafkaConsumerWithStringDeserializer(topicKeyedTransactionOutput,"group-id-reward");

        long timeConsumerResponseStart=System.currentTimeMillis();

        List<ConsumerRecord<String, String>> consumerRecords = new ArrayList<>(transactionInputNumber);
        int counter = 0;
        while(counter<transactionInputNumber) {
            if(System.currentTimeMillis()-timeConsumerResponseStart>maxWaitingMs){
                org.junit.jupiter.api.Assertions.fail("timeout of %d ms expired".formatted(maxWaitingMs));
            }

            ConsumerRecords<String, String> published = consumer.poll(Duration.ofMillis(7000));
            for (ConsumerRecord<String, String> record : published) {
                consumerRecords.add(record);
                counter++;
            }
        }

        long timeEnd=System.currentTimeMillis();

        Assertions.assertEquals(transactionInputNumber,counter);
        List<ConsumerRecord<String, String>> transactionsPartition0 = consumerRecords.stream().filter(r->r.partition() == 0).toList();
        List<ConsumerRecord<String, String>> transactionsPartition1 = consumerRecords.stream().filter(r->r.partition() == 1).toList();

        List<String> userIdsInPartition0 = transactionsPartition0.stream().map(ConsumerRecord::key).distinct().toList();
        List<String> userIdsInPartition1 = transactionsPartition1.stream().map(ConsumerRecord::key).distinct().toList();
        Assertions.assertEquals(transactionInputNumber, transactionsPartition0.size()+transactionsPartition1.size());

        Assertions.assertNotEquals(userIdsInPartition0,userIdsInPartition1);
        System.out.printf("""
            ************************
            Receive %d transactions
            Receive %d transactions in partition 0 with the follow userId: %s
            Receive %d transactions in partition 1 with the follow userId: %s
            ************************
            Test Completed in %d millis
            ************************
            """,
                transactionInputNumber,
                transactionsPartition0.size(), userIdsInPartition0,
                transactionsPartition1.size(), userIdsInPartition1,
                timeEnd-timePublishTransactionsStart
        );
    }


    @Test
    void trxProcessorHpanNotValid() throws  RuntimeException{
        int transactionInputInvalidHpanNumber = 100;
        long maxWaitingMs=30000;

        //region send a TransactionsDTO
        long timePublishTransactionsStart=System.currentTimeMillis();

        IntStream.range(0, transactionInputInvalidHpanNumber)
                .mapToObj(TransactionDTOFaker::mockInstance)
                .forEach(t-> publishIntoEmbeddedKafka(topicTransactionInput,null,null,t));
        //endregion

        Consumer<String,String> consumerForTransaction = getEmbeddedKafkaConsumer(topicTransactionOutput,"group-id-transaction");


        long timeConsumerRejectionResponseStart=System.currentTimeMillis();
        List<ConsumerRecord<String, String>> consumerRejectionRecords = new ArrayList<>(transactionInputInvalidHpanNumber);
        int counterRejection = 0;
        while(counterRejection<transactionInputInvalidHpanNumber) {
            if(System.currentTimeMillis()-timeConsumerRejectionResponseStart>maxWaitingMs){
                org.junit.jupiter.api.Assertions.fail("timeout of %d ms expired".formatted(maxWaitingMs));
            }
            ConsumerRecords<String, String> published = consumerForTransaction.poll(Duration.ofMillis(7000));
            for (ConsumerRecord<String, String> record : published) {
                consumerRejectionRecords.add(record);
                counterRejection++;
            }
        }

        long timeEnd=System.currentTimeMillis();

        Assertions.assertEquals(transactionInputInvalidHpanNumber,counterRejection);
        Assertions.assertEquals(transactionInputInvalidHpanNumber,consumerRejectionRecords.size());
        consumerRejectionRecords.stream().map(r-> {
                    try {
                        return objectMapper.readValue(r.value(), TransactionRejectedDTO.class);
                    } catch (JsonProcessingException e) {
                        Assertions.fail("object mapper exception");
                        throw new RuntimeException(e);
                    }
                }
        ).forEach(t->{
            Assertions.assertNull(t.getUserId());
            Assertions.assertEquals("REJECTED",t.getStatus());
            Assertions.assertFalse(t.getRejectionReasons().isEmpty());
            Assertions.assertTrue(t.getRejectionReasons().contains("REJECTION_FOR_HPAN_NOT_VALID"));
        });

        System.out.printf("""
            ************************
            Expected %d and receive %d transactions
            ************************
            Test Completed in %d millis
            ************************
            """,
                transactionInputInvalidHpanNumber, counterRejection,
                timeEnd-timePublishTransactionsStart
        );
    }

    private void setInitiativeHpanForIncomingTransactions() {
        List<HpanInitiatives> hpanInitiativeList =IntStream.range(0, hpanInitiativeNumber)
                .mapToObj(HpanInitiativesFaker::mockInstance)
                .toList();
        hpanInitiativesRepository.saveAll(hpanInitiativeList).subscribe(i-> log.info(i.toString()));

        long[] countSaved={0};
        //noinspection ConstantConditions
        waitFor(()->(countSaved[0]=hpanInitiativesRepository.count().block()) >= hpanInitiativeNumber, ()->"Expected %d saved rules, read %d".formatted(hpanInitiativeNumber, countSaved[0]), 15, 1000);

    }
}