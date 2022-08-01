package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class UserIdSplitterMediatorImpl implements UserIdSplitterMediator{
    private final RetrieveUserIdService retrieveUserIdService;
    private final MessagePreparationService messagePreparationService;

    public UserIdSplitterMediatorImpl(RetrieveUserIdService retrieveUserIdService, MessagePreparationService messagePreparationService) {
        this.retrieveUserIdService = retrieveUserIdService;
        this.messagePreparationService = messagePreparationService;
    }

    @Override
    public Flux<Message<TransactionEnrichedDTO>> execute(Flux<TransactionDTO> transactionDTOFlux) {
        return transactionDTOFlux.map(this.retrieveUserIdService::updateTransaction)
                .map(messagePreparationService::getMessage);
    }
}
