package it.gov.pagopa.splitter.repository;

import it.gov.pagopa.splitter.model.HpanInitiatives;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface HpanInitiativesRepository extends ReactiveMongoRepository<HpanInitiatives,String> {
}
