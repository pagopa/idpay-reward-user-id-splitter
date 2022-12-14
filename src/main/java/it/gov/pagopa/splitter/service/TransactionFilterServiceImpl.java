package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.service.filter.TransactionFilter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionFilterServiceImpl implements TransactionFilterService{

    private final List<TransactionFilter> filters;

    public TransactionFilterServiceImpl(List<TransactionFilter> filters) {
        this.filters = filters;
    }

    @Override
    public Boolean filter(TransactionDTO transactionDTO) {
        return filters.stream().allMatch(f -> f.test(transactionDTO));
    }
}
