package it.gov.pagopa.splitter.event.processor;

import it.gov.pagopa.splitter.BaseIntegrationTest;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import it.gov.pagopa.splitter.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;


@Slf4j
class TransactionProcessorTest extends BaseIntegrationTest {
    @Value("${app.filter.mccExcluded}")
    List<String> mccExcluded;
    private final int hpanInitiativeNumber = 5;

    @Test
    void trxProcessor() {
        int transactionInputNumber =101;
        int transactionInputInvalidHpanNumber=50;

        String mccValid= "2000";

        setInitiativeHpanForIncomingTransactions();

        //region send a TransactionsDTO
        long timePublishTransactionsStart=System.currentTimeMillis();
        IntStream.range(0, transactionInputInvalidHpanNumber)
                .mapToObj(i -> TransactionDTOFaker.mockInstanceBuilder(i+hpanInitiativeNumber)
                        .mcc(i%2==0 ? mccExcluded.get(new Random().nextInt(mccExcluded.size())) : mccValid)
                        .build())
                .forEach(t-> publishIntoEmbeddedKafka(topicTransactionInput,null,null,t));

        IntStream.range(0, transactionInputNumber)
                .mapToObj(n->TransactionDTOFaker.mockInstanceBuilder(n)
                        .hpan("HPAN%s".formatted(n%hpanInitiativeNumber))
                        .mcc(n%2==0 ? mccExcluded.get(new Random().nextInt(mccExcluded.size())):mccValid)
                        .build())
                .forEach( t -> publishIntoEmbeddedKafka(topicTransactionInput,null, null,t));
        //endregion

        long timeReadValidTransactionStart=System.currentTimeMillis();
        List<ConsumerRecord<String, String>> consumerRecords = checkTopicTransactionOutput(transactionInputNumber/2, topicKeyedTransactionOutput);
        long timeReadValidTransactionEnd=System.currentTimeMillis();

        List<ConsumerRecord<String, String>> transactionsPartition0 = consumerRecords.stream().filter(r->r.partition() == 0).toList();
        List<ConsumerRecord<String, String>> transactionsPartition1 = consumerRecords.stream().filter(r->r.partition() == 1).toList();

        List<String> userIdsInPartition0 = transactionsPartition0.stream().map(ConsumerRecord::key).distinct().toList();
        List<String> userIdsInPartition1 = transactionsPartition1.stream().map(ConsumerRecord::key).distinct().toList();
        Assertions.assertEquals(transactionInputNumber/2, transactionsPartition0.size()+transactionsPartition1.size());

        long timeReadTransactionRejectedEStart=System.currentTimeMillis();
        List<ConsumerRecord<String, String>> checkTopicTransactionRejectionResult = checkTopicTransactionOutput(transactionInputInvalidHpanNumber/2, topicTransactionRejectedOutput);
        long timeReadTransactionRejectedEnd=System.currentTimeMillis();

        Assertions.assertEquals(transactionInputInvalidHpanNumber/2,checkTopicTransactionRejectionResult.size());
        Assertions.assertNotEquals(userIdsInPartition0,userIdsInPartition1);

        long timeEnd=System.currentTimeMillis();

        System.out.printf("""
            ************************
            Elaborate %d transactions
            Message with MCC valid: %d
            
            Receive %d transactions in rewards topic
            Receive %d transactions in partition 0 with the follow userId: %s
            Receive %d transactions in partition 1 with the follow userId: %s
            Time to read from reward topic: %d
            
            Receive %d transactions in rejected topic
            Time to read from rejected topic: %d
            ************************
            Test Completed in %d millis
            ************************
            """,
                transactionInputNumber+transactionInputInvalidHpanNumber,
                (transactionInputNumber/2)+(transactionInputInvalidHpanNumber/2),
                transactionInputNumber/2,
                transactionsPartition0.size(), userIdsInPartition0,
                transactionsPartition1.size(), userIdsInPartition1,
                timeReadValidTransactionEnd-timeReadValidTransactionStart,
                transactionInputInvalidHpanNumber/2,
                timeReadTransactionRejectedEnd-timeReadTransactionRejectedEStart,
                timeEnd-timePublishTransactionsStart
        );
    }

    private List<ConsumerRecord<String, String>> checkTopicTransactionOutput(int expectedTransactionNumber,String topicName) {
        long maxWaitingMs=30000;

        List<ConsumerRecord<String, String>> consumerRecords = new ArrayList<>(expectedTransactionNumber);
        int counter = 0;
        try(Consumer<String, String> consumer = getEmbeddedKafkaConsumer(topicName,"group-id-%s".formatted(topicName))) {

            long timeConsumerResponseStart = System.currentTimeMillis();


            while (counter < expectedTransactionNumber) {
                if (System.currentTimeMillis() - timeConsumerResponseStart > maxWaitingMs) {
                    Assertions.fail("timeout of %d ms expired".formatted(maxWaitingMs));
                }

                ConsumerRecords<String, String> published = consumer.poll(Duration.ofMillis(7000));
                for (ConsumerRecord<String, String> record : published) {
                    consumerRecords.add(record);
                    counter++;
                }
            }
        }
        Assertions.assertEquals(expectedTransactionNumber,counter);
        return consumerRecords;
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