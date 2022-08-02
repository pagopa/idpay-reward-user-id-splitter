package it.gov.pagopa.splitter.event.processor;

import it.gov.pagopa.splitter.BaseIntegrationTest;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import it.gov.pagopa.splitter.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;


@Slf4j
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
                .toList()
                .forEach( t -> publishIntoEmbeddedKafka(topicTransactionInput,null, null,t));
        //endregion

        Consumer<String, String> consumer = getEmbeddedKafkaConsumerWithStringDeserializer(topicKeyedTransactionOutput,"group-id");
        consumer.seekToBeginning(List.of(new TopicPartition(topicKeyedTransactionOutput,0)));

        long timeConsumerResponse=System.currentTimeMillis();

        List<ConsumerRecord<String, String>> consumerRecords = new ArrayList<>(transactionInputNumber);
        int counter = 0;
        while(counter<transactionInputNumber) {
            if(System.currentTimeMillis()-timeConsumerResponse>maxWaitingMs){
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