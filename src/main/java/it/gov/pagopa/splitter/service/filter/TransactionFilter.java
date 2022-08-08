package it.gov.pagopa.splitter.service.filter;

import it.gov.pagopa.splitter.dto.TransactionDTO;

import java.util.function.Predicate;

/**
 * Filter to skip transaction
 * */
public interface TransactionFilter extends Predicate<TransactionDTO> {
}
