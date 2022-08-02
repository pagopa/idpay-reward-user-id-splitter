package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.Objects;

@Slf4j
class MessageKeyedPreparationImplTest {

    @Test
    void testApply() {
        // Given
        String userId = "USERID";
        TransactionEnrichedDTO transactionEnrichedDTO = new TransactionEnrichedDTO();
        transactionEnrichedDTO.setUserId(userId);
        transactionEnrichedDTO.setHpan("HPAN");

        MessageKeyedPreparation messageKeyedPreparation = new MessageKeyedPreparationImpl();
        // When
        Message<TransactionEnrichedDTO> result = messageKeyedPreparation.apply(transactionEnrichedDTO);

        // Then
        log.info(Objects.requireNonNull(result.getHeaders().get(KafkaHeaders.MESSAGE_KEY)).toString());
        Assertions.assertEquals(userId, result.getHeaders().get(KafkaHeaders.MESSAGE_KEY));
    }
}