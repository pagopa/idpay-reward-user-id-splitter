package it.gov.pagopa.splitter.event.processor;

import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.service.UserIdSplitterMediator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Function;

@Configuration
@Slf4j
public class TransactionProcessor {
    /**
     * Read from the topic ${KAFKA_RTD_TOPIC} and publish to topic ${KAFKA_TRANSACTION_USER_ID_SPLITTER_TOPIC}
     * */
    @Bean
    public Function<Flux<Message<String>>,Flux<Message<TransactionEnrichedDTO>>> trxProcessor(UserIdSplitterMediator userIdSplitterMediator){
        return userIdSplitterMediator::execute;
    }
}
