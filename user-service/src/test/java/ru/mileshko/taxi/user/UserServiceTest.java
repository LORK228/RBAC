package ru.mileshko.taxi.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private SetOperations<String, String> setOps;

    private UserService service;

    private final OffsetDateTime now = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        service = new UserService(repository, redis);
    }

    private void givenRedisSetOps() {
        when(redis.opsForSet()).thenReturn(setOps);
    }

    @Test
    void createPassenger_delegatesToRepository() {
        CreatePassengerRequest req = new CreatePassengerRequest("N", "n@e.com", "+1");
        Passenger p = new Passenger(1, "N", "n@e.com", "+1", now);
        when(repository.createPassenger(req)).thenReturn(p);

        assertThat(service.createPassenger(req)).isEqualTo(p);
    }

    @Test
    void getPassenger_notFound_throws404() {
        when(repository.findPassenger(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPassenger(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void createDriver_addsIdToRedisSet() {
        givenRedisSetOps();
        CreateDriverRequest req = new CreateDriverRequest("D", "d@e.com", "+1", "LIC");
        Driver d = new Driver(5, "D", "d@e.com", "+1", "LIC", DriverStatus.AVAILABLE, now);
        when(repository.createDriver(req)).thenReturn(d);

        Driver result = service.createDriver(req);

        assertThat(result.id()).isEqualTo(5);
        verify(setOps).add("drivers:available", "5");
    }

    @Test
    void availableDrivers_usesCacheWhenNonEmpty() {
        givenRedisSetOps();
        when(setOps.members("drivers:available")).thenReturn(Set.of("1", "2"));
        Driver a = new Driver(1, "A", "a@e.com", "+1", "L1", DriverStatus.AVAILABLE, now);
        Driver b = new Driver(2, "B", "b@e.com", "+1", "L2", DriverStatus.AVAILABLE, now);
        when(repository.findDriver(1L)).thenReturn(Optional.of(a));
        when(repository.findDriver(2L)).thenReturn(Optional.of(b));

        assertThat(service.availableDrivers()).containsExactlyInAnyOrder(a, b);
        verify(repository, never()).findAvailableDrivers();
    }

    @Test
    void availableDrivers_cacheMiss_loadsFromDbAndPopulatesRedis() {
        givenRedisSetOps();
        when(setOps.members("drivers:available")).thenReturn(Set.of());
        Driver a = new Driver(1, "A", "a@e.com", "+1", "L1", DriverStatus.AVAILABLE, now);
        when(repository.findAvailableDrivers()).thenReturn(List.of(a));

        assertThat(service.availableDrivers()).containsExactly(a);
        verify(setOps).add("drivers:available", "1");
    }

    @Test
    void availableDrivers_cachedIdButDriverBusy_excludedFromResult() {
        givenRedisSetOps();
        when(setOps.members("drivers:available")).thenReturn(Set.of("1"));
        Driver busy = new Driver(1, "A", "a@e.com", "+1", "L1", DriverStatus.BUSY, now);
        when(repository.findDriver(1L)).thenReturn(Optional.of(busy));

        assertThat(service.availableDrivers()).isEmpty();
    }

    @Test
    void updateStatus_refreshesCacheWhenDriverBecomesOffline() {
        givenRedisSetOps();
        Driver updated = new Driver(3, "A", "a@e.com", "+1", "L1", DriverStatus.OFFLINE, now);
        when(repository.updateDriverStatus(3L, DriverStatus.OFFLINE)).thenReturn(Optional.of(updated));

        assertThat(service.updateStatus(3L, DriverStatus.OFFLINE)).isEqualTo(updated);
        verify(setOps).remove("drivers:available", "3");
    }

    @Test
    void updateStatus_available_addsToCache() {
        givenRedisSetOps();
        Driver updated = new Driver(3, "A", "a@e.com", "+1", "L1", DriverStatus.AVAILABLE, now);
        when(repository.updateDriverStatus(3L, DriverStatus.AVAILABLE)).thenReturn(Optional.of(updated));

        service.updateStatus(3L, DriverStatus.AVAILABLE);

        verify(setOps).add("drivers:available", "3");
    }

    @Test
    void allocateDriver_success_refreshesCache() {
        givenRedisSetOps();
        Driver d = new Driver(9, "A", "a@e.com", "+1", "L1", DriverStatus.BUSY, now);
        when(repository.allocateDriver()).thenReturn(Optional.of(d));

        assertThat(service.allocateDriver()).isEqualTo(d);
        verify(setOps).remove("drivers:available", "9");
    }

    @Test
    void allocateDriver_none_throwsConflict() {
        when(repository.allocateDriver()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.allocateDriver())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        CreateUserAuthRequest req = new CreateUserAuthRequest("a@a.com", "h", "N", "PASSENGER", 1L);
        when(repository.findUserAuthByEmail("a@a.com")).thenReturn(Optional.of(
                new UserAuth(1, "a@a.com", "h", "N", "PASSENGER", 1L, now)));

        assertThatThrownBy(() -> service.createUserAuth(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");
    }

    @Test
    void login_invalidPassword_throwsUnauthorized() {
        when(repository.findUserAuthByEmail("a@a.com")).thenReturn(Optional.of(
                new UserAuth(1, "a@a.com", "secret", "N", "PASSENGER", 1L, now)));

        assertThatThrownBy(() -> service.login(new LoginRequest("a@a.com", "wrong")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }

    @Test
    void login_success_returnsResponse() {
        when(repository.findUserAuthByEmail("a@a.com")).thenReturn(Optional.of(
                new UserAuth(1, "a@a.com", "secret", "N", "PASSENGER", 42L, now)));

        UserAuthResponse r = service.login(new LoginRequest("a@a.com", "secret"));

        assertThat(r.email()).isEqualTo("a@a.com");
        assertThat(r.javaUserId()).isEqualTo(42L);
    }
}
