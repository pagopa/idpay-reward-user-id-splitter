package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.service.filter.TransactionFilter;
import it.gov.pagopa.splitter.dto.TransactionDTO;

/**
 * This component will take a {@link TransactionDTO} and will test an against all the {@link TransactionFilter} configured
 * */
public interface TransactionFilterService {
    Boolean filter(TransactionDTO transactionDTO);
}
