package it.gov.pagopa.splitter.test.utils;

import it.gov.pagopa.splitter.dto.TransactionDTO;
import it.gov.pagopa.splitter.dto.TransactionEnrichedDTO;
import it.gov.pagopa.splitter.dto.TransactionRejectedDTO;
import org.junit.jupiter.api.Assertions;
import org.springframework.util.ReflectionUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TestUtils {
    private TestUtils() {
    }

    /**
     * It will assert not null on all o's fields
     */
    public static void checkNotNullFields(Object o, String... excludedFields) {
        Set<String> excludedFieldsSet = new HashSet<>(Arrays.asList(excludedFields));
        org.springframework.util.ReflectionUtils.doWithFields(o.getClass(),
                getFieldCallback(o),
                f -> !excludedFieldsSet.contains(f.getName()));

    }

    public static void checkTransactionNotNullFields(TransactionDTO o, String... excludedFields){
        checkNotNullFields(o,excludedFields);
    }

    public static void checkTransactionEnrichedNotNullFields(TransactionEnrichedDTO o, String... excludedFields) {
        checkNotNullFields(o,excludedFields);
        checkTransactionNotNullFields(o,excludedFields);
    }

    public static void checkTransactionRejectedNotNullFields(TransactionRejectedDTO o, String... excludedFields) {
        checkNotNullFields(o,excludedFields);
        checkTransactionEnrichedNotNullFields(o,excludedFields);
    }

    private static ReflectionUtils.FieldCallback getFieldCallback(Object o) {
        return f -> {
            f.setAccessible(true);
            Assertions.assertNotNull(f.get(o), "The field %s of the input object of type %s is null!".formatted(f.getName(), o.getClass()));
        };
    }





}
