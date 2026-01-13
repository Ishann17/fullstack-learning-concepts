package com.ishan.user_service.specification;

import com.ishan.user_service.model.User;
import org.springframework.data.jpa.domain.Specification;

/**
 * UserSpecification contains reusable, composable filter conditions
 * for dynamically building database queries.
 * Each method in this class represents ONE filtering rule.
 * IMPORTANT:
 * - Specification does NOT execute queries
 * - It only DESCRIBES the WHERE condition
 * - Spring Data JPA later combines and executes them
 */
public class UserSpecification {

    /**
     * Builds a specification to filter users by name (case-insensitive, partial match).
     * Business meaning:
     * "Return users whose name CONTAINS the given text, ignoring case."
     * Example:
     * Input name = "ishan"
     * Matches:
     * - Ishan Singh
     * - ishan raghav
     * - ISHAN VERMA
     * @param name the text to search inside user names
     * @return Specification<User> representing the WHERE clause condition
     */
    public static Specification<User> hasName(String name) {

        /*
         * Specification is a functional interface.
         * We return a lambda implementation of:
         *
         * Predicate toPredicate(Root<User> root,
         *                       CriteriaQuery<?> query,
         *                       CriteriaBuilder criteriaBuilder)
         */
        return (root, query, criteriaBuilder) ->

                /*
                 * criteriaBuilder.like(...) creates a SQL LIKE condition
                 *
                 * This translates roughly to:
                 * WHERE LOWER(name) LIKE '%ishan%'
                 */
                criteriaBuilder.like(

                        /*
                         * criteriaBuilder.lower(...)
                         * Converts the column value to lowercase
                         * This ensures CASE-INSENSITIVE searching
                         */
                        criteriaBuilder.lower(

                                /*
                                 * root.get("name")
                                 * - root represents the User table
                                 * - "name" refers to the 'name' column in the User entity
                                 */
                                root.get("name")
                        ),

                        /*
                         * "%" + name.toLowerCase() + "%"
                         *
                         * % is a wildcard in SQL LIKE queries
                         * - %ishan% means "contains ishan anywhere"
                         *
                         * We also convert input to lowercase
                         * so both sides of comparison are normalized
                         */
                        "%" + name.toLowerCase() + "%"
                );
    }

    public static Specification<User> hasCity(String city){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("city")), "%" + city.toLowerCase() + "%" ));
    }
    public static Specification<User> hasState(String state){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("state")), "%" + state.toLowerCase() + "%" ));
    }
    public static Specification<User> hasAge(Integer age){
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("age"), age
                ));
    }
}

/*
*******IMPORTANT NOTE********
* “Why didn’t you make your Specification class implement Specification<User>?”
* Because we needed dynamic, combinable filters. Implementing Specification directly would
* force all logic into one toPredicate method.
* Returning small reusable Specifications allows clean composition using .and() and .or().
 */