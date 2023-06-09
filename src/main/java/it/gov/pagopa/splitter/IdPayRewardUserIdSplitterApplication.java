package it.gov.pagopa.splitter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class IdPayRewardUserIdSplitterApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdPayRewardUserIdSplitterApplication.class, args);
    }

}