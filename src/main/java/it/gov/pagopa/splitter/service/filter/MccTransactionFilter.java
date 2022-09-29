package it.gov.pagopa.splitter.service.filter;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@Order(0)
public class MccTransactionFilter implements TransactionFilter{

    private final Set<String> mccExcluded;

    public MccTransactionFilter(@Value("${app.filter.mccExcluded}") Set<String> mccExcluded) {
        this.mccExcluded = mccExcluded;
    }

    @Override
    public boolean test(TransactionDTO transactionDTO) {
        boolean mccAllowed = !mccExcluded.contains(transactionDTO.getMcc());
        if(!mccAllowed){
            log.info("[TRX_USERID_SPLITTER] Transaction discarded due to MCC");
        }
        return mccAllowed;
    }
}
