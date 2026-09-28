package com.pgs.user.service.service;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.pgs.user.service.dto.UserDto;
import com.pgs.user.service.dto.UserFilterDto;
import com.pgs.user.service.entity.User;
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

    private final UserRepository userRepository;

    /**
     * Creates a new user from the provided data.
     *
     * @param input the user data to create
     * @return the created user as a DTO
     */
    public UserDto createUser(UserDto input) {
        final User createdUser = User.builder()
                .name(input.getName())
                .surname(input.getSurname())
                .email(input.getEmail())
                .address(input.getAddress())
                .alerting(input.isAlerting())
                .energyAlertingThreshold(input.getEnergyAlertingThreshold())
                .build();

        final User saved = userRepository.save(createdUser);
        return toDto(saved);
    }

    /**
     * Retrieves all users.
     *
     * @return the list of users as DTOs
     */
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
    public Page<UserDto> getUsersPaginated(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size))
                .map(this::toDto);
    }

    /**
     * Searches users by optional filters, paginated.
     *
     * @param filter the filter criteria (name, surname, email, address, alerting, threshold)
     * @param page   the page index (zero-based)
     * @param size   the page size
     * @return a page of matching users as DTOs
     */
    public Page<UserDto> searchUsers(UserFilterDto filter, int page, int size) {
        Specification<User> spec = Stream.of(
                        UserSpecification.nameContains(filter.getName()),
                        UserSpecification.surnameContains(filter.getSurname()),
                        UserSpecification.emailContains(filter.getEmail()),
                        UserSpecification.addressContains(filter.getAddress()),
                        UserSpecification.alertingEquals(filter.getAlerting()),
                        UserSpecification.thresholdGreaterThanOrEqual(filter.getMinEnergyAlertingThreshold()))
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());

        return userRepository.findAll(spec, PageRequest.of(page, size))
                .map(this::toDto);
    }
    
    /**
     * Retrieves a user by ID, using cache when available.
     *
     * @param id the user ID
     * @return the user as a DTO, or {@code null} if not found
     */
    @Cacheable(value = "users", key = "#id")
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * Updates an existing user with the provided data and evicts the cache entry.
     *
     * @param id  the user ID
     * @param dto the updated user data
     * @throws IllegalArgumentException if the user is not found
     */
    @CacheEvict(value = "users", key = "#id")
    public void updateUser(Long id, UserDto dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setName(dto.getName());
        user.setSurname(dto.getSurname());
        user.setEmail(dto.getEmail());
        user.setAddress(dto.getAddress());
        user.setAlerting(dto.isAlerting());
        user.setEnergyAlertingThreshold(dto.getEnergyAlertingThreshold());

        userRepository.save(user);
    }

    /**
     * Deletes a user by ID and evicts the cache entry.
     *
     * @param id the user ID
     * @throws IllegalArgumentException if the user is not found
     */
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        userRepository.delete(user);
    }

    /**
     * Maps a {@link User} entity to a {@link UserDto}.
     *
     * @param user the user entity
     * @return the corresponding user DTO
     */
    private UserDto toDto(User user) {
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