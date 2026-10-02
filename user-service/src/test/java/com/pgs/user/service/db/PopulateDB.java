package com.pgs.user.service.db;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.pgs.user.service.entity.User;
import com.pgs.user.service.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@Disabled
class PopulateDB {

    private static final int NUMBER_OF_USERS = 10;

    @Autowired
    private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

    @Test
    void addUsersToDB() {
        for (int i = 1; i <= NUMBER_OF_USERS; i++) {
            User user = User.builder()
                    .name("User" + i)
                    .surname("Surname" + i)
                    .email("user" + i + "@example.com")
                    .address(i + " Example St")
                    .alerting(i % 2 == 0)
                    .energyAlertingThreshold(1000.0 + i)
                    .build();
            userRepository.save(user);
        }
        log.info("User Repository populated successfully");
    }

}