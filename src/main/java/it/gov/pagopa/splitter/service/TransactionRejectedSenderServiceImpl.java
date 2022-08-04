package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2RejectionMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

@Service
public class TransactionRejectedSenderServiceImpl implements TransactionRejectedSenderService {
    private final Sinks.Many<TransactionRejectedDTO> trxRejectedMany;
    private final Transaction2RejectionMapper transaction2RejectionMapper;

    public TransactionRejectedSenderServiceImpl(Sinks.Many<TransactionRejectedDTO> trxRejectedMany, Transaction2RejectionMapper transaction2RejectionMapper) {
        this.trxRejectedMany = trxRejectedMany;
        this.transaction2RejectionMapper = transaction2RejectionMapper;
    }

    @Override
    public void sendTransactionRejected(TransactionDTO transactionDTO) {
        TransactionRejectedDTO transactionRejectedDTO = transaction2RejectionMapper.apply(transactionDTO,"REJECTION_FOR_HPAN_NOT_VALID");
        synchronized (trxRejectedMany)
        {
            trxRejectedMany.emitNext(transactionRejectedDTO, Sinks.EmitFailureHandler.FAIL_FAST);
        }
    }
}
