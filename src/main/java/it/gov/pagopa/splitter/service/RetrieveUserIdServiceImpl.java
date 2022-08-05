package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2EnrichedMapper;
import it.gov.pagopa.splitter.repository.HpanInitiativesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RetrieveUserIdServiceImpl implements RetrieveUserIdService {
    private final HpanInitiativesRepository hpanInitiativesRepository;
    private final Transaction2EnrichedMapper transaction2EnrichedMapper;
    private final SenderTransactionRejectedService senderTransactionRejectedService;

    public RetrieveUserIdServiceImpl(HpanInitiativesRepository hpanInitiativesRepository, Transaction2EnrichedMapper transaction2EnrichedMapper, SenderTransactionRejectedService senderTransactionRejectedService) {
        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.transaction2EnrichedMapper = transaction2EnrichedMapper;
        this.senderTransactionRejectedService = senderTransactionRejectedService;
    }

    @Override
    public Mono<TransactionEnrichedDTO> updateTransaction(TransactionDTO transactionDTO) {
        return hpanInitiativesRepository.findById(transactionDTO.getHpan())
                .map(h -> transaction2EnrichedMapper.apply(transactionDTO, h.getUserId()))
                .switchIfEmpty(Mono.defer(() -> {
                    senderTransactionRejectedService.sendTransactionRejected(transactionDTO);
                    return Mono.empty();
                }));

    }
}