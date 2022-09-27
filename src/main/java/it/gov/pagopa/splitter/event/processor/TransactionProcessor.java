package it.gov.pagopa.splitter.event.processor;

import it.gov.pagopa.splitter.service.UserIdSplitterMediator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.BindingCreatedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class TransactionProcessor{
    public static final String TRX_PROCESSOR_BINDING_NAME = "trxProcessor-in-0";

    private final UserIdSplitterMediator userIdSplitterMediator;


    public TransactionProcessor(UserIdSplitterMediator userIdSplitterMediator) {
        this.userIdSplitterMediator = userIdSplitterMediator;
    }

    /**
     * Read from the topic ${KAFKA_RTD_TOPIC} and publish to topic ${KAFKA_TRANSACTION_USER_ID_SPLITTER_TOPIC}
     * */
    @Bean
    public Consumer<Flux<Message<String>>> trxProcessor(){
        return userIdSplitterMediator::execute;
    }

    @EventListener(BindingCreatedEvent.class)
    public void onBindingCreatedEvent(BindingCreatedEvent event) {
        if (event.getSource() instanceof Binding<?> binding && TRX_PROCESSOR_BINDING_NAME.equals(binding.getBindingName())) {
            synchronized (this) {
                binding.start();
            }
        }
    }
}
