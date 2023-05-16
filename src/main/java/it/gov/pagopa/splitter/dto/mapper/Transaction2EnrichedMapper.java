package it.gov.pagopa.splitter.dto.mapper;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.model.HpanInitiatives;
import org.springframework.stereotype.Service;

import java.util.function.BiFunction;

@Service
public class Transaction2EnrichedMapper implements BiFunction<TransactionDTO, HpanInitiatives, TransactionEnrichedDTO> {
    @Override
    public TransactionEnrichedDTO apply(TransactionDTO transactionDTO, HpanInitiatives hpanInitiatives) {
        TransactionEnrichedDTO out = new TransactionEnrichedDTO();

        out.setIdTrxAcquirer(transactionDTO.getIdTrxAcquirer());
        out.setAcquirerCode(transactionDTO.getAcquirerCode());
        out.setTrxDate(transactionDTO.getTrxDate());
        out.setHpan(transactionDTO.getHpan());
        out.setOperationType(transactionDTO.getOperationType());
        out.setCircuitType(transactionDTO.getCircuitType());
        out.setIdTrxIssuer(transactionDTO.getIdTrxIssuer());
        out.setCorrelationId(transactionDTO.getCorrelationId());
        out.setAmount(transactionDTO.getAmount());
        out.setAmountCurrency(transactionDTO.getAmountCurrency());
        out.setMcc(transactionDTO.getMcc());
        out.setAcquirerId(transactionDTO.getAcquirerId());
        out.setMerchantId(transactionDTO.getMerchantId());
        out.setTerminalId(transactionDTO.getTerminalId());
        out.setBin(transactionDTO.getBin());
        out.setSenderCode(transactionDTO.getSenderCode());
        out.setFiscalCode(transactionDTO.getFiscalCode());
        out.setVat(transactionDTO.getVat());
        out.setPosType(transactionDTO.getPosType());
        out.setPar(transactionDTO.getPar());

        out.setUserId(hpanInitiatives.getUserId());
        out.setMaskedPan(hpanInitiatives.getMaskedPan());
        out.setBrandLogo(hpanInitiatives.getBrandLogo());
        out.setBrand(hpanInitiatives.getBrand());
        return out;
    }
}