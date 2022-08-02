package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;

/**
 * This component will retrieve userId to which the input hpan has been enabled
 * */
public interface RetrieveUserIdService {
    TransactionEnrichedDTO updateTransaction(TransactionDTO transactionDTO);
}