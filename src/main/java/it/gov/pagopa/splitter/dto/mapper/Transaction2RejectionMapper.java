package it.gov.pagopa.splitter.dto.mapper;

import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
import it.gov.pagopa.splitter.dto.TransactionDTO;
import org.springframework.stereotype.Service;

import java.util.function.BiFunction;

@Service
public class Transaction2RejectionMapper implements BiFunction<TransactionDTO,String, TransactionRejectedDTO> {

    @Override
    public TransactionRejectedDTO apply(TransactionDTO transactionDTO, String rejectionReason) {
        TransactionRejectedDTO out = new TransactionRejectedDTO();

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

        out.getRejectionReasons().add(rejectionReason);
        out.setStatus("REJECTED");
        return out;
    }
}
