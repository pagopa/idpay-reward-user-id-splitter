package it.gov.pagopa.splitter.event.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.splitter.BaseIntegrationTest;
import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
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
class TransactionRejectedProducerTest extends BaseIntegrationTest {

    @Test
    void trxRejectedProducer() {
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
            Assertions.assertEquals(List.of("REJECTION_FOR_HPAN_NOT_VALID"),t.getRejectionReasons());

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
}