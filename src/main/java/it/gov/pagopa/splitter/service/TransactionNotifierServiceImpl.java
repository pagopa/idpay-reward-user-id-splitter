package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Service
public class TransactionNotifierServiceImpl implements TransactionNotifierService{
    private final StreamBridge streamBridge;

    public TransactionNotifierServiceImpl(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    /** Declared just to let know Spring to connect the producer at startup */
    @Configuration
    static class TransactionNotifierProducerConfig {
        @Bean
        public Supplier<Flux<Message<TransactionEnrichedDTO>>> trxProcessorOut() {
            return Flux::empty;
        }
    }

    @Override
    public boolean notify(TransactionEnrichedDTO transaction) {
        return streamBridge.send("trxProcessorOut-out-0",
                buildMessage(transaction));
    }

    public static Message<TransactionEnrichedDTO> buildMessage(TransactionEnrichedDTO transaction){
        return MessageBuilder.withPayload(transaction)
                .setHeader(KafkaHeaders.KEY,transaction.getUserId()).build();
    }
}
