package it.gov.pagopa.splitter.service;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.dto.mapper.Transaction2EnrichedMapper;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import it.gov.pagopa.splitter.repository.HpanInitiativesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class RetrieveUserIdServiceImpl implements RetrieveUserIdService{
    private final HpanInitiativesRepository hpanInitiativesRepository;
    private final Transaction2EnrichedMapper transaction2EnrichedMapper;
    private final TransactionRejectedSenderService transactionRejectedSenderService;

    public RetrieveUserIdServiceImpl(HpanInitiativesRepository hpanInitiativesRepository, Transaction2EnrichedMapper transaction2EnrichedMapper, TransactionRejectedSenderService transactionRejectedSenderService) {
        this.hpanInitiativesRepository = hpanInitiativesRepository;
        this.transaction2EnrichedMapper = transaction2EnrichedMapper;
        this.transactionRejectedSenderService = transactionRejectedSenderService;
    }

    @Override
    public TransactionEnrichedDTO updateTransaction(TransactionDTO transactionDTO) {
        Mono<HpanInitiatives> hpan = hpanInitiativesRepository.findById(transactionDTO.getHpan());
        if(Boolean.TRUE.equals(hpan.hasElement().block())){
            return hpan.map(h -> transaction2EnrichedMapper.apply(transactionDTO,h.getUserId())).block();
        } else {
            transactionRejectedSenderService.sendTransactionRejected(transactionDTO);
            return null;
        }
    }
}