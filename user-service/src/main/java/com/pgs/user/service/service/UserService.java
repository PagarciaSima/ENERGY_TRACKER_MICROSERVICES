package com.pgs.user.service.service;

import java.util.List;
import java.util.Objects;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pgs.user.service.dto.PageResponse;
import com.pgs.user.service.dto.UserDto;
import com.pgs.user.service.dto.UserFilterDto;
import com.pgs.user.service.entity.User;
import com.pgs.user.service.exception.UserAlreadyExistsException;
import com.pgs.user.service.exception.UserNotFoundException;
import com.pgs.user.service.repository.UserRepository;
import com.pgs.user.service.repository.UserSpecification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service layer for managing users.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String CACHE_USERS = "users";

    private final UserRepository userRepository;

    /**
     * Creates a new user from the provided data.
     *
     * @param input the user data to create
     * @return the created user as a DTO
     * @throws UserAlreadyExistsException if a user with the same email already exists
     */
    @Transactional
    public UserDto createUser(UserDto input) {
        if (userRepository.existsByEmail(input.getEmail())) {
            throw new UserAlreadyExistsException(input.getEmail());
        }

        User createdUser = User.builder()
                .name(input.getName())
                .surname(input.getSurname())
                .email(input.getEmail())
                .address(input.getAddress())
                .alerting(input.isAlerting())
                .energyAlertingThreshold(input.getEnergyAlertingThreshold())
                .build();

        User saved = userRepository.save(createdUser);
        log.debug("Created user with id={}", saved.getId());
        return toDto(saved);
    }

    /**
     * Retrieves all users.
     *
     * @return the list of users as DTOs
     */
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Retrieves a paginated list of users.
     *
     * @param page the page index (zero-based)
     * @param size the page size
     * @return a page of users as DTOs
     */
    @Transactional(readOnly = true)
    public PageResponse<UserDto> getUsersPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.of(userRepository.findAll(pageable).map(this::toDto));
    }

    /**
     * Searches users by optional filters, paginated.
     *
     * @param filter the filter criteria (name, surname, email, address, alerting, threshold)
     * @param page   the page index (zero-based)
     * @param size   the page size
     * @return a page of matching users as DTOs
     */
    @Transactional(readOnly = true)
    public PageResponse<UserDto> searchUsers(UserFilterDto filter, int page, int size) {
        Specification<User> spec = buildSpecification(filter);
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.of(userRepository.findAll(spec, pageable).map(this::toDto));
    }

    /**
     * Retrieves a user by ID, using cache when available.
     *
     * @param id the user ID
     * @return the user as a DTO
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_USERS, key = "#id")
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    /**
     * Updates an existing user with the provided data and evicts the cache entry.
     *
     * @param id  the user ID
     * @param dto the updated user data
     * @return the updated user as a DTO
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional
    @CacheEvict(value = CACHE_USERS, key = "#id")
    public UserDto updateUser(Long id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setAddress(dto.getAddress());
        user.setAlerting(dto.isAlerting());
        user.setEnergyAlertingThreshold(dto.getEnergyAlertingThreshold());

        // userRepository.save(user)not needed here because the entity is managed by EntityManager
        log.debug("Updated user with id={}", id);
        return toDto(user);
    }

    /**
     * Deletes a user by ID and evicts the cache entry.
     *
     * @param id the user ID
     * @throws UserNotFoundException if the user does not exist
     */
    @Transactional
    @CacheEvict(value = CACHE_USERS, key = "#id")
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
        log.debug("Deleted user with id={}", id);
    }

    /**
     * Builds the JPA specification from the filter, skipping null/blank fields.
     *
     * @param filter the filter criteria
     * @return the combined specification (never {@code null})
     */
    private Specification<User> buildSpecification(UserFilterDto filter) {
    	Specification<User> spec = (root, query, cb) -> cb.conjunction();
        if (filter == null) {
            return spec;
        }

        spec = spec.and(UserSpecification.nameContains(filter.getName()))
                   .and(UserSpecification.surnameContains(filter.getSurname()))
                   .and(UserSpecification.emailContains(filter.getEmail()))
                   .and(UserSpecification.addressContains(filter.getAddress()))
                   .and(UserSpecification.alertingEquals(filter.getAlerting()))
                   .and(UserSpecification.thresholdGreaterThanOrEqual(filter.getMinEnergyAlertingThreshold()));

        return spec;
    }

    /**
     * Maps a {@link User} entity to a {@link UserDto}.
     *
     * @param user the user entity
     * @return the corresponding user DTO
     */
    private UserDto toDto(User user) {
        Objects.requireNonNull(user, "user must not be null");
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .address(user.getAddress())
                .alerting(user.isAlerting())
                .energyAlertingThreshold(user.getEnergyAlertingThreshold())
                .build();
    }
}