package it.gov.pagopa.splitter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.utils.Utils;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class UserIdSplitterMediatorImpl implements UserIdSplitterMediator {
    private final RetrieveUserIdService retrieveUserIdService;
    private final MessageKeyedPreparation messageKeyedPreparation;
    private final TransactionFilterService transactionFilterService;
    private final ErrorNotifierService errorNotifierService;

    private final ObjectReader objectReader;

    public UserIdSplitterMediatorImpl(RetrieveUserIdService retrieveUserIdService, MessageKeyedPreparation messageKeyedPreparation, TransactionFilterService transactionFilterService, ErrorNotifierService errorNotifierService, ObjectMapper objectMapper) {
        this.retrieveUserIdService = retrieveUserIdService;
        this.messageKeyedPreparation = messageKeyedPreparation;
        this.transactionFilterService = transactionFilterService;
        this.errorNotifierService = errorNotifierService;

        this.objectReader = objectMapper.readerFor(TransactionDTO.class);
    }

    @Override
    public Flux<Message<TransactionEnrichedDTO>> execute(Flux<Message<String>> messageFlux) {
        return messageFlux.flatMap(this::execute);
    }

    public Mono<Message<TransactionEnrichedDTO>> execute(Message<String> message) {
        return Mono.just(message)
                .mapNotNull(this::deserializeMessage)

                .filter(this.transactionFilterService::filter)
                .flatMap(this.retrieveUserIdService::resolveUserId)
                .mapNotNull(this.messageKeyedPreparation)

                .onErrorResume(e -> {
                    errorNotifierService.notifyTransactionEvaluation(message, "An error occurred evaluating transaction", true, e);
                    return Mono.empty();
                });
    }

    private TransactionDTO deserializeMessage(Message<String> message) {
        return Utils.deserializeMessage(message, objectReader, e -> errorNotifierService.notifyTransactionEvaluation(message, "Unexpected JSON", true, e));
    }
}
