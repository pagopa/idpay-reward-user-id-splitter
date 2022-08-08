package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import org.springframework.messaging.Message;

import java.util.function.Function;

/**
 * This component will take a {@link TransactionEnrichedDTO} and it will create a keyed message to send
 * */
public interface MessageKeyedPreparation extends Function<TransactionEnrichedDTO, Message<TransactionEnrichedDTO>> {
}