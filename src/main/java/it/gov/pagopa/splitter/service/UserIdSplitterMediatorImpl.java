package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class UserIdSplitterMediatorImpl implements UserIdSplitterMediator{
    private final RetrieveUserIdService retrieveUserIdService;
    private final MessageKeyedPreparation messageKeyedPreparation;
    private final TransactionFilterService transactionFilterService;

    public UserIdSplitterMediatorImpl(RetrieveUserIdService retrieveUserIdService, MessageKeyedPreparation messageKeyedPreparation, TransactionFilterService transactionFilterService) {
        this.retrieveUserIdService = retrieveUserIdService;
        this.messageKeyedPreparation = messageKeyedPreparation;
        this.transactionFilterService = transactionFilterService;
    }

    @Override
    public Flux<Message<TransactionEnrichedDTO>> execute(Flux<TransactionDTO> transactionDTOFlux) {
        return transactionDTOFlux
                .filter(this.transactionFilterService::filter)
                .flatMap(this.retrieveUserIdService::resolveUserId)
                .mapNotNull(this.messageKeyedPreparation);

    }
}
