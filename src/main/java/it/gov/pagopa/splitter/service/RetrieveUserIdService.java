package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import reactor.core.publisher.Mono;

/**
 * This component will retrieve userId to which the input hpan has been enabled
 * */
public interface RetrieveUserIdService {
    Mono<TransactionEnrichedDTO> updateTransaction(TransactionDTO transactionDTO);
}