package com.isup.repository;

import com.isup.entity.AccessRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccessRuleRepository extends JpaRepository<AccessRule, Long> {
    List<AccessRule> findByEmployeeNo(String employeeNo);
    List<AccessRule> findByRuleType(String ruleType);
    void deleteByEmployeeNo(String employeeNo);
}
