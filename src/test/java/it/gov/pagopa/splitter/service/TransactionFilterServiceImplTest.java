package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.service.filter.MccTransactionFilter;
import it.gov.pagopa.splitter.service.filter.TransactionFilter;
import it.gov.pagopa.splitter.test.fakers.TransactionDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class TransactionFilterServiceImplTest {
    private TransactionFilterServiceImpl transactionFilterService;


    @BeforeEach
    void setUp() {
        List<TransactionFilter> transactionFilterList = List.of(
                new MccTransactionFilter(Set.of("")));
        transactionFilterService = new TransactionFilterServiceImpl(transactionFilterList);
    }

    @Test
    void filter() {
        TransactionDTO transactionDTO = TransactionDTOFaker.mockInstance(1);

        Boolean filter = transactionFilterService.filter(transactionDTO);

        Assertions.assertTrue(filter);

    }
}