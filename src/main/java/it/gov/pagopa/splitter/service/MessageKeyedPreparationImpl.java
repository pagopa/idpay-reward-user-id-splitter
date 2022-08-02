package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageKeyedPreparationImpl implements MessageKeyedPreparation {
    @Override
    public Message<TransactionEnrichedDTO> apply(TransactionEnrichedDTO transactionEnrichedDTO) {
        return MessageBuilder.withPayload(transactionEnrichedDTO)
                .setHeader(KafkaHeaders.MESSAGE_KEY,transactionEnrichedDTO.getUserId()).build();
    }
}
