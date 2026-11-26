package com.igraded.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    List<AppUser> findAllByOrderByFullNameAsc();

    List<AppUser> findByRoleOrderByFullNameAsc(Role role);

    List<AppUser> findByRoleAndDepartmentIdOrderByFullNameAsc(Role role, Long departmentId);

    long countByDepartmentId(Long departmentId);
}
