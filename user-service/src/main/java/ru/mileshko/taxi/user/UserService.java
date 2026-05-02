package ru.mileshko.taxi.user;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
class UserService {
    private static final String AVAILABLE_DRIVERS_KEY = "drivers:available";

    private final UserRepository repository;
    private final StringRedisTemplate redis;

    UserService(UserRepository repository, StringRedisTemplate redis) {
        this.repository = repository;
        this.redis = redis;
    }

    Passenger createPassenger(CreatePassengerRequest request) {
        return repository.createPassenger(request);
    }

    Passenger getPassenger(long id) {
        return repository.findPassenger(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Passenger not found"));
    }

    Driver createDriver(CreateDriverRequest request) {
        Driver driver = repository.createDriver(request);
        redis.opsForSet().add(AVAILABLE_DRIVERS_KEY, String.valueOf(driver.id()));
        return driver;
    }

    Driver getDriver(long id) {
        return repository.findDriver(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
    }

    List<Driver> availableDrivers() {
        Set<String> cachedIds = redis.opsForSet().members(AVAILABLE_DRIVERS_KEY);
        if (cachedIds != null && !cachedIds.isEmpty()) {
            return cachedIds.stream()
                    .map(Long::parseLong)
                    .map(repository::findDriver)
                    .flatMap(OptionalDriver::stream)
                    .filter(driver -> driver.status() == DriverStatus.AVAILABLE)
                    .toList();
        }
        List<Driver> drivers = repository.findAvailableDrivers();
        drivers.forEach(driver -> redis.opsForSet().add(AVAILABLE_DRIVERS_KEY, String.valueOf(driver.id())));
        return drivers;
    }

    Driver updateStatus(long id, DriverStatus status) {
        Driver driver = repository.updateDriverStatus(id, status)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found"));
        refreshDriverCache(driver);
        return driver;
    }

    @Transactional
    Driver allocateDriver() {
        Driver driver = repository.allocateDriver()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No available drivers"));
        refreshDriverCache(driver);
        return driver;
    }

    private void refreshDriverCache(Driver driver) {
        String id = String.valueOf(driver.id());
        if (driver.status() == DriverStatus.AVAILABLE) {
            redis.opsForSet().add(AVAILABLE_DRIVERS_KEY, id);
        } else {
            redis.opsForSet().remove(AVAILABLE_DRIVERS_KEY, id);
        }
    }
}

final class OptionalDriver {
    private OptionalDriver() {
    }

    static java.util.stream.Stream<Driver> stream(java.util.Optional<Driver> driver) {
        return driver.stream();
    }
}
