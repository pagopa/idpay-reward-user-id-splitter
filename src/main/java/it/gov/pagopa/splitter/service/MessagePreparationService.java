package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import org.springframework.messaging.Message;

/**
 * This component will take a {@link TransactionEnrichedDTO} and it will create a keyed message to send
 * */
public interface MessagePreparationService {
    Message<TransactionEnrichedDTO> getMessage(TransactionEnrichedDTO transactionEnrichedDTO);
}