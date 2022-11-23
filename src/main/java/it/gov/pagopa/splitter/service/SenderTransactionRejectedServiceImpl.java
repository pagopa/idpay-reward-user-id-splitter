package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2RejectionMapper;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
public class SenderTransactionRejectedServiceImpl implements SenderTransactionRejectedService {
    private final Transaction2RejectionMapper transaction2RejectionMapper;
    private final StreamBridge streamBridge;

    public SenderTransactionRejectedServiceImpl(Transaction2RejectionMapper transaction2RejectionMapper, StreamBridge streamBridge) {
        this.transaction2RejectionMapper = transaction2RejectionMapper;
        this.streamBridge = streamBridge;
    }

    @Override
    public void sendTransactionRejected(TransactionDTO transactionDTO) {
        TransactionRejectedDTO transactionRejectedDTO = transaction2RejectionMapper.apply(transactionDTO,"INVALID_HPAN");
        streamBridge.send("trxRejectedProducer-out-0", transactionRejectedDTO);
    }
}
