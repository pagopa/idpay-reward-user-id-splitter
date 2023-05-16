package it.gov.pagopa.splitter.event.processor;

import it.gov.pagopa.splitter.BaseIntegrationTest;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2EnrichedMapper;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import it.gov.pagopa.splitter.repository.HpanInitiativesRepository;
import it.gov.pagopa.splitter.service.ErrorNotifierServiceImpl;
import it.gov.pagopa.splitter.service.TransactionNotifierService;
import it.gov.pagopa.splitter.test.fakers.HpanInitiativesFaker;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import it.gov.pagopa.splitter.test.utils.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.util.Pair;
import org.springframework.test.context.TestPropertySource;

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
@TestPropertySource(properties = {
        "logging.level.it.gov.pagopa.splitter.service.UserIdSplitterMediatorServiceImpl=WARN",
        "logging.level.it.gov.pagopa.splitter.service.filter=WARN",
        "logging.level.it.gov.pagopa.splitter.service.RetrieveUserIdServiceImpl=WARN",
        "logging.level.it.gov.pagopa.splitter.service.SenderTransactionRejectedServiceImpl=WARN",
        "logging.level.it.gov.pagopa.splitter.service.TransactionFilterServiceImpl=WARN",
        "logging.level.it.gov.pagopa.splitter.service.BaseKafkaConsumer=WARN",
        "logging.level.it.gov.pagopa.splitter.utils.PerformanceLogger=WARN",
})
class TransactionProcessorTest extends BaseIntegrationTest {
    @Value("${app.filter.mccExcluded}")
    List<String> mccExcluded;
    private final int hpanInitiativeNumber = 5;

    private final String mccValid= "2000";

    @Autowired
    private HpanInitiativesRepository hpanInitiativesRepository;
    @Autowired
    private Transaction2EnrichedMapper transaction2EnrichedMapper;

    @SpyBean
    private TransactionNotifierService transactionNotifierServiceSpy;

    @AfterEach
    void cleanData() {
        hpanInitiativesRepository.deleteAll().block();
    }

    @Test
    void trxProcessor() {

        int trxWhitoutInititativeHpanInDBNumber =50;
        int trxWithInititativeHpanInDBNumber=100;

        setInitiativeHpanForIncomingTransactions();

        List<String> transactionEvents = new ArrayList<>();
        transactionEvents.addAll(getTrxForHpanNotPresent(trxWhitoutInititativeHpanInDBNumber));
        transactionEvents.addAll(errorUseCases.stream().map(u -> u.getFirst().get()).toList());
        transactionEvents.addAll(getTrxFordHpanPresent(trxWithInititativeHpanInDBNumber));

        long timePublishTransactionsStart=System.currentTimeMillis();
        transactionEvents.forEach(t-> publishIntoEmbeddedKafka(topicTransactionInput,null,null,t));
        publishIntoEmbeddedKafka(topicTransactionInput, List.of(new RecordHeader(ErrorNotifierServiceImpl.ERROR_MSG_HEADER_APPLICATION_NAME, "OTHERAPPNAME".getBytes(StandardCharsets.UTF_8))), null, "OTHERAPPMESSAGE");

        long timeReadValidTransactionStart=System.currentTimeMillis();
        List<ConsumerRecord<String, String>> consumerRecords = consumeMessages(topicKeyedTransactionOutput, trxWithInititativeHpanInDBNumber/2, 30000);
        long timeReadValidTransactionEnd=System.currentTimeMillis();

        List<ConsumerRecord<String, String>> transactionsPartition0 = consumerRecords.stream().filter(r->r.partition() == 0).toList();
        List<ConsumerRecord<String, String>> transactionsPartition1 = consumerRecords.stream().filter(r->r.partition() == 1).toList();

        List<String> userIdsInPartition0 = transactionsPartition0.stream().map(ConsumerRecord::key).distinct().toList();
        List<String> userIdsInPartition1 = transactionsPartition1.stream().map(ConsumerRecord::key).distinct().toList();
        Assertions.assertEquals(trxWithInititativeHpanInDBNumber/2, transactionsPartition0.size()+transactionsPartition1.size());

        long timeReadTransactionRejectedEStart=System.currentTimeMillis();
        List<ConsumerRecord<String, String>> checkTopicTransactionRejectionResult = consumeMessages(topicTransactionRejectedOutput, trxWhitoutInititativeHpanInDBNumber/2, 30000);
        long timeReadTransactionRejectedEnd=System.currentTimeMillis();

        Assertions.assertEquals(trxWhitoutInititativeHpanInDBNumber/2,checkTopicTransactionRejectionResult.size());
        Assertions.assertNotEquals(userIdsInPartition0,userIdsInPartition1);

        long timeEnd=System.currentTimeMillis();

        checkErrorsPublished(errorUseCases.size(), 3000, errorUseCases);

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
                trxWithInititativeHpanInDBNumber+trxWhitoutInititativeHpanInDBNumber+errorUseCases.size(),
                (trxWithInititativeHpanInDBNumber/2)+(trxWhitoutInititativeHpanInDBNumber/2),
                trxWithInititativeHpanInDBNumber/2,
                transactionsPartition0.size(), userIdsInPartition0,
                transactionsPartition1.size(), userIdsInPartition1,
                timeReadValidTransactionEnd-timeReadValidTransactionStart,
                trxWhitoutInititativeHpanInDBNumber/2,
                timeReadTransactionRejectedEnd-timeReadTransactionRejectedEStart,
                timeEnd-timePublishTransactionsStart
        );

        checkOffsets(transactionEvents.size()+1, trxWithInititativeHpanInDBNumber/2); // +1 due to other applicationName useCase
    }

    private List<String> getTrxFordHpanPresent(int trxInputNotPresentHpanNumber) {
        return IntStream.range(0, trxInputNotPresentHpanNumber)
                .mapToObj(n -> TransactionDTOFaker.mockInstanceBuilder(n)
                        .hpan("HPAN%s".formatted(n % hpanInitiativeNumber))
                        .mcc(n % 2 == 0 ? mccExcluded.get(new Random().nextInt(mccExcluded.size())) : mccValid)
                        .build())
                .map(TestUtils::jsonSerializer)
                .toList();
    }

    private List<String> getTrxForHpanNotPresent(int trxNotPresentdHpanNumber) {
        return IntStream.range(0, trxNotPresentdHpanNumber)
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

        final String failingRewardPublishingIdTrxAcquirer = "FAILING_REWARD_PUBLISHING";
        TransactionDTO failingRewardPublishing = TransactionDTOFaker.mockInstanceBuilder(500)
                .hpan("HPAN%s".formatted(500 % hpanInitiativeNumber))
                .mcc(mccValid)
                .correlationId("CORRELATIONID2")
                .idTrxAcquirer(failingRewardPublishingIdTrxAcquirer)
                .build();

        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doReturn(false).when(transactionNotifierServiceSpy).notify(Mockito.argThat(i -> failingRewardPublishingIdTrxAcquirer.equals(i.getIdTrxAcquirer())));
                    return TestUtils.jsonSerializer(failingRewardPublishing);
                },
                errorMessage -> {
                    TransactionEnrichedDTO transactionEnrichedFailingReward = retrievePayloadTrxEnriched(transaction2EnrichedMapper, failingRewardPublishing);
                    checkErrorMessageHeaders(topicKeyedTransactionOutput,"", errorMessage, "[TRX_USERID_SPLITTER] An error occurred while publishing the transaction evaluation result", TestUtils.jsonSerializer(transactionEnrichedFailingReward),transactionEnrichedFailingReward.getUserId(),false, false);
                })
        );

        final String exceptionWhenRewardPublishIdTrxAcquirer = "FAILING_REWARD_PUBLISHING_DUE_EXCEPTION";
        TransactionDTO exceptionWhenRewardPublish = TransactionDTOFaker.mockInstanceBuilder(501)
                .hpan("HPAN%s".formatted(501 % hpanInitiativeNumber))
                .mcc(mccValid)
                .correlationId("CORRELATIONID3")
                .idTrxAcquirer(exceptionWhenRewardPublishIdTrxAcquirer)
                .build();
        errorUseCases.add(Pair.of(
                () -> {
                    Mockito.doThrow(new KafkaException()).when(transactionNotifierServiceSpy).notify(Mockito.argThat(i -> exceptionWhenRewardPublishIdTrxAcquirer.equals(i.getIdTrxAcquirer())));
                    return TestUtils.jsonSerializer(exceptionWhenRewardPublish);
                },
                errorMessage -> {
                    TransactionEnrichedDTO transactionEnrichedExceptionRewardPublish = retrievePayloadTrxEnriched(transaction2EnrichedMapper, exceptionWhenRewardPublish);
                    checkErrorMessageHeaders(topicKeyedTransactionOutput, "", errorMessage, "[TRX_USERID_SPLITTER] An error occurred while publishing the transaction evaluation result", TestUtils.jsonSerializer(transactionEnrichedExceptionRewardPublish),transactionEnrichedExceptionRewardPublish.getUserId(),false, false);
                }
        ));
    }

    private TransactionEnrichedDTO retrievePayloadTrxEnriched(Transaction2EnrichedMapper transaction2EnrichedMapper, TransactionDTO failingRewardPublishing) {
        return hpanInitiativesRepository.findById(failingRewardPublishing.getHpan())
                .map(e -> transaction2EnrichedMapper.apply(failingRewardPublishing,e)).block();
    }

    private void checkErrorMessageHeaders(ConsumerRecord<String, String> errorMessage, String errorDescription, String expectedPayload) {
        checkErrorMessageHeaders(topicTransactionInput, groupIdTrxProcessorConsumer, errorMessage, errorDescription, expectedPayload,null);
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