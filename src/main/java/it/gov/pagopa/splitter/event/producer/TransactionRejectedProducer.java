package it.gov.pagopa.splitter.event.producer;

import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Supplier;

@Configuration
@Slf4j
public class TransactionRejectedProducer {
    @Bean
    public Sinks.Many<TransactionRejectedDTO> trxRejectedMany(){
        return Sinks.many().unicast().onBackpressureBuffer();
    }

    @Bean
    public Supplier<Flux<TransactionRejectedDTO>> trxRejectedProducer(Sinks.Many<TransactionRejectedDTO> many) {
        return () -> many.asFlux()
                .doOnNext(trx -> log.info("Sending message: {}", trx))
                .doOnError(e -> log.error("Error encountered", e));
    }

}
