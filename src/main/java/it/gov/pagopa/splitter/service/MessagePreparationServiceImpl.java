package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class MessagePreparationServiceImpl implements MessagePreparationService {
    @Override
    public Message<TransactionEnrichedDTO> getMessage(TransactionEnrichedDTO transactionEnrichedDTO) {
        return MessageBuilder.withPayload(transactionEnrichedDTO)
                .setHeader(KafkaHeaders.MESSAGE_KEY,transactionEnrichedDTO.getUserId().getBytes(StandardCharsets.UTF_8)).build();
    }
}
