package it.gov.pagopa.splitter.event.processor;

import it.gov.pagopa.splitter.BaseIntegrationTest;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import it.gov.pagopa.splitter.repository.HpanInitiativesRepository;
import it.gov.pagopa.splitter.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.splitter.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.splitter.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Slf4j
class TransactionProcessorTest extends BaseIntegrationTest {
    @Value("${app.filter.mccExcluded}")
    List<String> mccExcluded;
    private final int hpanInitiativeNumber = 5;

    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;

    @AfterEach
    void cleanData() {
        hpanInitiativesRepository.deleteAll().block();
    }

    @Test
    void trxProcessor() {

        int transactionInititativeHpanNumber =100;
        int transactionNotInititativeHpanNumber=50;

        String mccValid= "2000";

        setInitiativeHpanForIncomingTransactions();

        List<String> transactionEvents = new ArrayList<>();
        transactionEvents.addAll(getValidHpanTrxs(transactionInititativeHpanNumber, mccValid));
        transactionEvents.addAll(errorUseCases.stream().map(u -> u.getFirst().get()).toList());
        transactionEvents.addAll(getInvalidHpanTrxs(transactionNotInititativeHpanNumber, mccValid));

        long timePublishTransactionsStart=System.currentTimeMillis();
        transactionEvents.forEach(t-> publishIntoEmbeddedKafka(topicTransactionInput,null,null,t));
        publishIntoEmbeddedKafka(topicTransactionInput, List.of(new RecordHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");

        long timeReadValidTransactionStart=System.currentTimeMillis();
        List<ConsumerRecord<String, String>> consumerRecords = consumeMessages(topicKeyedTransactionOutput, transactionNotInititativeHpanNumber/2, 30000);
        long timeReadValidTransactionEnd=System.currentTimeMillis();

        List<ConsumerRecord<String, String>> transactionsPartition0 = consumerRecords.stream().filter(r->r.partition() == 0).toList();
        List<ConsumerRecord<String, String>> transactionsPartition1 = consumerRecords.stream().filter(r->r.partition() == 1).toList();

        List<String> userIdsInPartition0 = transactionsPartition0.stream().map(ConsumerRecord::key).distinct().toList();
        List<String> userIdsInPartition1 = transactionsPartition1.stream().map(ConsumerRecord::key).distinct().toList();
        Assertions.assertEquals(transactionNotInititativeHpanNumber/2, transactionsPartition0.size()+transactionsPartition1.size());

        long timeReadTransactionRejectedEStart=System.currentTimeMillis();
        List<ConsumerRecord<String, String>> checkTopicTransactionRejectionResult = consumeMessages(topicTransactionRejectedOutput, transactionInititativeHpanNumber/2, 30000);
        long timeReadTransactionRejectedEnd=System.currentTimeMillis();

        Assertions.assertEquals(transactionInititativeHpanNumber/2,checkTopicTransactionRejectionResult.size());
        Assertions.assertNotEquals(userIdsInPartition0,userIdsInPartition1);

        long timeEnd=System.currentTimeMillis();

        checkErrorsPublished(2, 3000, errorUseCases);

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
                transactionNotInititativeHpanNumber+transactionInititativeHpanNumber+errorUseCases.size(),
                (transactionNotInititativeHpanNumber/2)+(transactionInititativeHpanNumber/2),
                transactionNotInititativeHpanNumber/2,
                transactionsPartition0.size(), userIdsInPartition0,
                transactionsPartition1.size(), userIdsInPartition1,
                timeReadValidTransactionEnd-timeReadValidTransactionStart,
                transactionInititativeHpanNumber/2,
                timeReadTransactionRejectedEnd-timeReadTransactionRejectedEStart,
                timeEnd-timePublishTransactionsStart
        );

        checkOffsets(transactionEvents.size()+1, transactionNotInititativeHpanNumber/2); // +1 due to other applicationName useCase
    }

    private List<String> getInvalidHpanTrxs(int transactionInputInvalidHpanNumber, String mccValid) {
        return IntStream.range(0, transactionInputInvalidHpanNumber)
                .mapToObj(n -> TransactionDTOFaker.mockInstanceBuilder(n)
                        .hpan("HPAN%s".formatted(n % hpanInitiativeNumber))
                        .mcc(n % 2 == 0 ? mccExcluded.get(new Random().nextInt(mccExcluded.size())) : mccValid)
                        .build())
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private List<String> getValidHpanTrxs(int transactionInputValidHpanNumber, String mccValid) {
        return IntStream.range(0, transactionInputValidHpanNumber)
                .mapToObj(i -> TransactionDTOFaker.mockInstanceBuilder(i + hpanInitiativeNumber)
                        .mcc(i % 2 == 0 ? mccExcluded.get(new Random().nextInt(mccExcluded.size())) : mccValid)
                        .build())
                .map(TestUtils::jsonSerializer)
                .toList();
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

    //region not valid useCases
    // all use cases configured must have a unique id recognized by the regexp getErrorUseCaseIdPatternMatch
    protected Pattern getErrorUseCaseIdPatternMatch() {
        return Pattern.compile("\"correlationId\":\"CORRELATIONID([0-9]+)\"");
    }

    private final List<Pair<Supplier<String>, Consumer<ConsumerRecord<String, String>>>> errorUseCases = new ArrayList<>();
    {
        String useCaseJsonNotExpected = "{\"correlationId\":\"CORRELATIONID0\",unexpectedStructure:0}";
        errorUseCases.add(Pair.of(
                () -> useCaseJsonNotExpected,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[TRX_USERID_SPLITTER] Unexpected JSON", useCaseJsonNotExpected)
        ));

        String jsonNotValid = "{\"correlationId\":\"CORRELATIONID1\",invalidJson";
        errorUseCases.add(Pair.of(
                () -> jsonNotValid,
                errorMessage -> checkErrorMessageHeaders(errorMessage, "[TRX_USERID_SPLITTER] Unexpected JSON", jsonNotValid)
        ));
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicTransactionInput, groupIdTrxProcessorConsumer, errorMessage, errorDescription, expectedPayload);
    }
    //endregion

    protected void checkOffsets(long expectedReadMessages, long exptectedPublishedResults){
        long timeStart = System.currentTimeMillis();
        final Map<TopicPartition, OffsetAndMetadata> srcCommitOffsets = checkCommittedOffsets(topicTransactionInput, groupIdTrxProcessorConsumer,expectedReadMessages, 10, 1000);
        long timeCommitChecked = System.currentTimeMillis();
        final Map<TopicPartition, Long> destPublishedOffsets = checkPublishedOffsets(topicKeyedTransactionOutput, exptectedPublishedResults);
        long timePublishChecked = System.currentTimeMillis();
        System.out.printf("""
                        ************************
                        Time occurred to check committed offset: %d millis
                        Time occurred to check published offset: %d millis
                        ************************
                        Source Topic Committed Offsets: %s
                        Dest Topic Published Offsets: %s
                        ************************
                        """,
                timeCommitChecked - timeStart,
                timePublishChecked - timeCommitChecked,
                srcCommitOffsets,
                destPublishedOffsets
        );
    }
}