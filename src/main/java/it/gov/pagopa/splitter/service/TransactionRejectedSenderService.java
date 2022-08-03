package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;

/**
 * this component will take a {@link TransactionDTO} with not valid hpan, and it will send with rejection reason
 * */
public interface TransactionRejectedSenderService {
    void sendTransactionRejected(TransactionDTO transactionDTO);
}
