package io.customerservice.customerservice.repository;

import io.customerservice.customerservice.entity.AgreementType;
import io.customerservice.customerservice.entity.CustomerAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerAgreementRepository extends JpaRepository<CustomerAgreement, Long>{
    List<CustomerAgreement> findByCustomerId(Long customerId);
    Optional<CustomerAgreement> findTopByCustomerIdAndAgreementTypeOrderByAudit_CreatedAtDesc(
            Long customerId, AgreementType type);

    @Query("""
      SELECT ca FROM CustomerAgreement ca
      WHERE ca.customer.id = :customerId
      AND ca.accepted = true
      AND ca.id = (
          SELECT MAX(ca2.id) FROM CustomerAgreement ca2
          WHERE ca2.customer.id = :customerId
          AND ca2.agreementType = ca.agreementType
      )
      """)
    List<CustomerAgreement> findActiveByCustomerId(@Param("customerId") Long customerId);

}
