package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2RejectionMapper;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
public class SenderTransactionRejectedServiceImpl implements SenderTransactionRejectedService {
    private final Transaction2RejectionMapper transaction2RejectionMapper;
    private final StreamBridge streamBridge;

    public SenderTransactionRejectedServiceImpl(Transaction2RejectionMapper transaction2RejectionMapper, StreamBridge streamBridge) {
        this.transaction2RejectionMapper = transaction2RejectionMapper;
        this.streamBridge = streamBridge;
    }

    /** Declared just to let know Spring to connect the producer at startup */
    @Configuration
    static class TrxRejectedProducerConfig {
        @Bean
        public Supplier<Flux<Message<TransactionDTO>>> trxRejectedProducer() {
            return Flux::empty;
        }
    }

    @Override
    public void sendTransactionRejected(TransactionDTO transactionDTO) {
        TransactionRejectedDTO transactionRejectedDTO = transaction2RejectionMapper.apply(transactionDTO,"INVALID_HPAN");
        streamBridge.send("trxRejectedProducer-out-0", transactionRejectedDTO);
    }
}
