package com.ishan.user_service.repository;

import com.ishan.user_service.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    //Pagination & Sorting both are done at DB Level. Sorting comes first then Pagination
    Page<User> findByAgeBetween(int minAge, int maxAge, Pageable pageable);

// Keyset Pagination (Next-batch fetch)
// Why: OFFSET pagination slows down as data grows because DB must skip rows again & again.
// This query fetches the next chunk using "id > lastId", which is fast because id is indexed.
//
// @Param is used to bind Java method arguments to named query placeholders (:lastId, :size)
// reliably (even if compiler/runtime doesn't retain parameter names).
//
// Example: lastId=5000, size=1000 → returns the next 1000 users after id 5000.
    @Query(value = "SELECT * FROM users WHERE id > :lastId ORDER BY id ASC LIMIT :size", nativeQuery = true)
    List<User> fetchUserUsingNextBatch(@Param("lastId") Integer lastId,
                                       @Param("size") Integer size);

// Used only for progress % (single query)
// Avoids guessing completion during long exports
    @Query(value = "SELECT COUNT(*) FROM user", nativeQuery = true)
    long countAllUsers();
}
