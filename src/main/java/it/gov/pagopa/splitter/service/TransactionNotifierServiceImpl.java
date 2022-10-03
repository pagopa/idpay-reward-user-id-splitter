package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class TransactionNotifierServiceImpl implements TransactionNotifierService{
    private final StreamBridge streamBridge;

    public TransactionNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public boolean notify(TransactionEnrichedDTO transaction) {
        return streamBridge.send("trxProcessor-out-0",
                MessageBuilder.withPayload(transaction)
                        .setHeader(KafkaHeaders.MESSAGE_KEY,transaction.getUserId()).build());
    }
}
