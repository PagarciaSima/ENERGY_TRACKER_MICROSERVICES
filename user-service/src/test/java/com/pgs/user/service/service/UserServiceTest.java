package com.pgs.user.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.pgs.user.service.dto.PageResponse;
import com.pgs.user.service.dto.UserDto;
import com.pgs.user.service.dto.UserFilterDto;
import com.pgs.user.service.entity.User;
import com.pgs.user.service.exception.UserAlreadyExistsException;
import com.pgs.user.service.exception.UserNotFoundException;
import com.pgs.user.service.repository.UserRepository;

/**
 * Unit tests for {@link UserService} business logic. The {@link UserRepository}
 * is mocked so no database is required. Caching and transactional behaviour are
 * covered separately (see {@link UserServiceCacheTest}).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String NAME = "Ana";
    private static final String SURNAME = "García";
    private static final String EMAIL = "ana.garcia@example.com";
    private static final String ADDRESS = "Calle Mayor 5, Madrid";
    private static final boolean ALERTING = true;
    private static final double THRESHOLD = 3200.5;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    /* ------------------------------------------------------------------ */
    /*  Test data factories                                                */
    /* ------------------------------------------------------------------ */

    private static User existingUser(Long id) {
        return User.builder()
                .id(id)
                .name(NAME).surname(SURNAME).email(EMAIL).address(ADDRESS)
                .alerting(ALERTING).energyAlertingThreshold(THRESHOLD)
                .build();
    }

    /** Request DTO as it would arrive via HTTP: no id yet. */
    private static UserDto newUserRequest() {
        return UserDto.builder()
                .name(NAME).surname(SURNAME).email(EMAIL).address(ADDRESS)
                .alerting(ALERTING).energyAlertingThreshold(THRESHOLD)
                .build();
    }

    private static UserDto existingUserDto(Long id) {
        return UserDto.builder()
                .id(id)
                .name(NAME).surname(SURNAME).email(EMAIL).address(ADDRESS)
                .alerting(ALERTING).energyAlertingThreshold(THRESHOLD)
                .build();
    }

    /* ------------------------------------------------------------------ */
    /*  createUser                                                         */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        void whenEmailAlreadyExists_throwsConflictAndDoesNotSave() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(newUserRequest()))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessage("User already exists with email: " + EMAIL);

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void whenEmailIsFree_persistsAndReturnsMappedDto() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(existingUser(1L));

            UserDto result = userService.createUser(newUserRequest());

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo(NAME);
            assertThat(result.getSurname()).isEqualTo(SURNAME);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getAddress()).isEqualTo(ADDRESS);
            assertThat(result.isAlerting()).isTrue();
            assertThat(result.getEnergyAlertingThreshold()).isEqualTo(THRESHOLD);

            verify(userRepository).save(any(User.class));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  read operations                                                    */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        void mapsEveryEntityToDto() {
            when(userRepository.findAll()).thenReturn(List.of(existingUser(1L), existingUser(2L)));

            List<UserDto> result = userService.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result).map(UserDto::getId).containsExactly(1L, 2L);
        }

        @Test
        void returnsEmptyListWhenNoUsers() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertThat(userService.getAllUsers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUsersPaginated")
    class GetUsersPaginated {

        @Test
        void returnsPageResponseWithMappedContent() {
            Page<User> page = new PageImpl<>(List.of(existingUser(1L)), PageRequest.of(0, 2), 1L);
            when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

            PageResponse<UserDto> result = userService.getUsersPaginated(0, 2);

            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
            assertThat(result.isEmpty()).isFalse();
            assertThat(result.getContent()).map(UserDto::getId).containsExactly(1L);
        }
    }

    @Nested
    @DisplayName("searchUsers")
    class SearchUsers {

        @SuppressWarnings("unchecked")
		@Test
        void buildsSpecificationAndMapsResults() {
            Page<User> page = new PageImpl<>(List.of(existingUser(1L)), PageRequest.of(0, 10), 1L);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

            UserFilterDto filter = UserFilterDto.builder().name("ana").build();
            PageResponse<UserDto> result = userService.searchUsers(filter, 0, 10);

            assertThat(result.getContent()).map(UserDto::getId).containsExactly(1L);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @SuppressWarnings("unchecked")
		@Test
        void withNullFilterStillReturnsPage() {
            Page<User> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0L);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(empty);

            PageResponse<UserDto> result = userService.searchUsers(null, 0, 10);

            assertThat(result.isEmpty()).isTrue();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        void whenFound_returnsMappedDto() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser(1L)));

            UserDto result = userService.getUserById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        void whenMissing_throwsNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with id: 99");
        }
    }

    /* ------------------------------------------------------------------ */
    /*  updateUser                                                         */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        void updatesManagedEntityInPlaceAndReturnsMappedDto() {
            User existing = existingUser(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

            UserDto updates = UserDto.builder()
                    .name("Beatriz").surname("López").email("b.lopez@example.com")
                    .address("Gran Vía 1, Madrid").alerting(false).energyAlertingThreshold(2500.0)
                    .build();

            UserDto result = userService.updateUser(1L, updates);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Beatriz");
            assertThat(result.getSurname()).isEqualTo("López");
            assertThat(result.getEmail()).isEqualTo("b.lopez@example.com");
            assertThat(result.getAddress()).isEqualTo("Gran Vía 1, Madrid");
            assertThat(result.isAlerting()).isFalse();
            assertThat(result.getEnergyAlertingThreshold()).isEqualTo(2500.0);

            assertThat(existing.getName()).isEqualTo("Beatriz");
            assertThat(existing.getEmail()).isEqualTo("b.lopez@example.com");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void whenMissing_throwsNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUser(99L, existingUserDto(99L)))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with id: 99");
        }
    }

    /* ------------------------------------------------------------------ */
    /*  setAlertingEnabled                                                 */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("setAlertingEnabled")
    class SetAlertingEnabled {

        @Test
        void whenExists_updatesOnlyAlertingFlag() {
            User existing = existingUser(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

            UserDto result = userService.setAlertingEnabled(1L, false);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.isAlerting()).isFalse();
            assertThat(result.getName()).isEqualTo(NAME);

            assertThat(existing.isAlerting()).isFalse();
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void whenMissing_throwsNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.setAlertingEnabled(99L, true))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with id: 99");
        }
    }

    /* ------------------------------------------------------------------ */
    /*  deleteUser                                                         */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        void whenExists_deletesById() {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.deleteUser(1L);

            verify(userRepository).deleteById(1L);
        }

        @Test
        void whenMissing_throwsNotFoundAndDoesNotDelete() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(99L))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("User not found with id: 99");

            verify(userRepository, never()).deleteById(99L);
        }
    }
}