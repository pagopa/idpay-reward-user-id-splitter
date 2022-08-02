package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

/**
 * This component given a {@link TransactionDTO}:
 * <ol>
 *     <li>enrich it with userId</li>
 *     <li>prepare the message with key to send to topic</li>
 * </ol>
 * */
public interface UserIdSplitterMediator {
    Flux<Message<TransactionEnrichedDTO>> execute(Flux<TransactionDTO> transactionDTOFlux);
}
