package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;

public interface TransactionNotifierService {
    boolean notify(TransactionEnrichedDTO transaction);
}
