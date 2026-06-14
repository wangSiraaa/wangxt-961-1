package com.charging.shed.repository;

import com.charging.shed.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    List<PricingRule> findByStatus(String status);

    List<PricingRule> findByShedId(Long shedId);

    List<PricingRule> findByShedIdAndStatus(Long shedId, String status);

    @Query("SELECT p FROM PricingRule p WHERE p.defaultRule = true AND p.status = 'ACTIVE'")
    Optional<PricingRule> findDefaultRule();

    @Query("SELECT p FROM PricingRule p WHERE (p.shedId = :shedId OR p.shedId IS NULL) " +
           "AND p.status = 'ACTIVE' ORDER BY p.shedId DESC NULLS LAST")
    List<PricingRule> findApplicableRules(@Param("shedId") Long shedId);

    List<PricingRule> findByCommunityIdAndStatus(String communityId, String status);

    @Query("SELECT p FROM PricingRule p WHERE (p.shedId = :shedId OR p.communityId = :communityId OR p.shedId IS NULL) " +
           "AND p.status = 'ACTIVE' ORDER BY p.shedId DESC NULLS LAST")
    List<PricingRule> findApplicableRulesByCommunity(@Param("shedId") Long shedId, @Param("communityId") String communityId);
}
