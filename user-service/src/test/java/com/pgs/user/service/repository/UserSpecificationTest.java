package com.pgs.user.service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

import com.pgs.user.service.entity.User;

/**
 * Slice tests for {@link UserSpecification} against an embedded H2 database.
 * <p>
 * The production datasource points to MySQL (see {@code application.properties}),
 * so the embedded replacement is disabled and H2 is configured explicitly in
 * MySQL compatibility mode. This lets the Flyway migration {@code V1__user_table.sql}
 * (MySQL-specific DDL) run unchanged. Each test is transactional and rolled back
 * by {@link DataJpaTest}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:user_spec_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration"
})
class UserSpecificationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final String EMAIL_ANA = "ana.garcia@example.com";
    private static final String EMAIL_LUIS = "luis.garcia@example.com";
    private static final String EMAIL_ANABELEN = "anabelen@example.com";

    /**
     * Persists the three fixture users used by most tests:
     * <ul>
     *     <li>Ana García (Madrid, alerting=true, 3200.5)</li>
     *     <li>Luis García (Barcelona, alerting=false, 1500.0)</li>
     *     <li>Ana Belén Sánchez (Madrid, alerting=true, 5000.0)</li>
     * </ul>
     */
    private void seedUsers() {
        persistUser("Ana", "García", EMAIL_ANA, "Calle Mayor 5, Madrid", true, 3200.5);
        persistUser("Luis", "García", EMAIL_LUIS, "Calle Sol 10, Barcelona", false, 1500.0);
        persistUser("Ana Belén", "Sánchez", EMAIL_ANABELEN, "Gran Vía 1, Madrid", true, 5000.0);
    }

    private void persistUser(String name, String surname, String email,
                             String address, boolean alerting, double threshold) {
        entityManager.persistAndFlush(User.builder()
                .name(name)
                .surname(surname)
                .email(email)
                .address(address)
                .alerting(alerting)
                .energyAlertingThreshold(threshold)
                .build());
    }

    private List<String> emailsOf(Specification<User> spec) {
        return userRepository.findAll(spec).stream()
                .map(User::getEmail)
                .toList();
    }

    /* ------------------------------------------------------------------ */
    /*  nameContains                                                       */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("nameContains")
    class NameContains {

        @Test
        void matchesNameContainingCaseInsensitive() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.nameContains("an")))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_ANABELEN);
        }

        @Test
        void blankValueReturnsAllUsers() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.nameContains("   ")))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_LUIS, EMAIL_ANABELEN);
        }

        @Test
        void nullValueReturnsAllUsers() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.nameContains(null)))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_LUIS, EMAIL_ANABELEN);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  surnameContains                                                    */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("surnameContains")
    class SurnameContains {

        @Test
        void matchesSurnameContainingCaseInsensitive() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.surnameContains("garcía")))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_LUIS);
        }

        @Test
        void nullValueReturnsAllUsers() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.surnameContains(null)))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_LUIS, EMAIL_ANABELEN);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  emailContains                                                      */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("emailContains")
    class EmailContains {

        @Test
        void matchesEmailContainingCaseInsensitive() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.emailContains("GARCIA@")))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_LUIS);
        }

        @Test
        void nonMatchingValueReturnsNoUsers() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.emailContains("unknown@"))).isEmpty();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  addressContains                                                    */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("addressContains")
    class AddressContains {

        @Test
        void matchesAddressContainingCaseInsensitive() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.addressContains("madrid")))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_ANABELEN);
        }

        @Test
        void blankValueReturnsAllUsers() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.addressContains("")))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_LUIS, EMAIL_ANABELEN);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  alertingEquals                                                     */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("alertingEquals")
    class AlertingEquals {

        @Test
        void trueValueMatchesOnlyUsersWithAlertingEnabled() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.alertingEquals(true)))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_ANABELEN);
        }

        @Test
        void falseValueMatchesOnlyUsersWithAlertingDisabled() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.alertingEquals(false)))
                    .containsExactlyInAnyOrder(EMAIL_LUIS);
        }

        @Test
        void nullValueReturnsAllUsers() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.alertingEquals(null)))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_LUIS, EMAIL_ANABELEN);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  thresholdGreaterThanOrEqual                                        */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("thresholdGreaterThanOrEqual")
    class ThresholdGreaterThanOrEqual {

        @Test
        void matchesThresholdGreaterThanOrEqual() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.thresholdGreaterThanOrEqual(3000.0)))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_ANABELEN);
        }

        @Test
        void boundaryValueIsIncluded() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.thresholdGreaterThanOrEqual(5000.0)))
                    .containsExactlyInAnyOrder(EMAIL_ANABELEN);
        }

        @Test
        void nullValueReturnsAllUsers() {
            seedUsers();

            assertThat(emailsOf(UserSpecification.thresholdGreaterThanOrEqual(null)))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_LUIS, EMAIL_ANABELEN);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Combined specifications                                            */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("combined specifications")
    class CombinedSpecifications {

        @Test
        void andCombinesPredicates() {
            seedUsers();

            Specification<User> spec = UserSpecification.surnameContains("García")
                    .and(UserSpecification.thresholdGreaterThanOrEqual(1600.0));

            assertThat(emailsOf(spec)).containsExactlyInAnyOrder(EMAIL_ANA);
        }

        @Test
        void andWithAlertingNarrowsBothMatches() {
            seedUsers();

            Specification<User> spec = UserSpecification.nameContains("Ana")
                    .and(UserSpecification.alertingEquals(true));

            assertThat(emailsOf(spec))
                    .containsExactlyInAnyOrder(EMAIL_ANA, EMAIL_ANABELEN);
        }
    }
}