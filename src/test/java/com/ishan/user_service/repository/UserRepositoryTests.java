package com.ishan.user_service.repository;


import com.ishan.user_service.model.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Loads only JPA layer (entities + repositories) for fast repository testing.
@DataJpaTest
// Forces tests to use in-memory H2 DB instead of real DB (MySQL/Postgres).
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
public class UserRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveUserTest(){
        User user = User.builder()
                    .firstName("test")
                    .age(28)
                    .email("test123@kok.com")
                    .city("ddun")
                    .state("uk")
                    .build();

        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isNotNull();
    }

    @Test
    void saveAll_shouldFail_whenDuplicateEmails(){

        User user1 = User.builder()
                .firstName("testOne")
                .age(28)
                .email("test@kok.com")
                .city("ddun")
                .state("uk")
                .build();


        User user2 = User.builder()
                .firstName("testTwo")
                .age(33)
                .email("test@kok.com")
                .city("haridwar")
                .state("uk")
                .build();

        assertThatThrownBy(()->{
            userRepository.saveAll(List.of(user1, user2));
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void fetchUserUsingNextBatch_shouldReturnNextUsersAfterLastId_sortedAndLimited(){

        User u1 = createTestUsers("test1@hit.com");
        User u2 = createTestUsers("test2@hit.com");
        User u3 = createTestUsers("test3@hit.com");
        User u4 = createTestUsers("test4@hit.com");
        User u5 = createTestUsers("test5@hit.com");
        User u6 = createTestUsers("test6@hit.com");


        userRepository.saveAllAndFlush(List.of(u1,u2,u3,u4, u5, u6));

        Integer lastId = u2.getId();

        List<User> batch = userRepository.fetchUserUsingNextBatch(lastId, 2);

        assertThat(batch).hasSize(2);
        assertThat(batch.getFirst().getId()).isGreaterThan(lastId);
        assertThat(batch.get(1).getId()).isGreaterThan(batch.get(0).getId());

    }

    private User createTestUsers(String email){

        User user = User.builder()
                .firstName("testOne")
                .age(28)
                .email(email)
                .city("ddun")
                .state("uk")
                .build();
        return user;
    }

}
