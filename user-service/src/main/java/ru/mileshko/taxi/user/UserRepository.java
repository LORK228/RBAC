package ru.mileshko.taxi.user;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
class UserRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Passenger> passengerMapper = this::mapPassenger;
    private final RowMapper<Driver> driverMapper = this::mapDriver;

    UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Passenger createPassenger(CreatePassengerRequest request) {
        return jdbc.queryForObject("""
                INSERT INTO passengers(name, email, phone)
                VALUES (?, ?, ?)
                RETURNING id, name, email, phone, created_at
                """, passengerMapper, request.name(), request.email(), request.phone());
    }

    Optional<Passenger> findPassenger(long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("""
                    SELECT id, name, email, phone, created_at
                    FROM passengers WHERE id = ?
                    """, passengerMapper, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    Driver createDriver(CreateDriverRequest request) {
        return jdbc.queryForObject("""
                INSERT INTO drivers(name, email, phone, license_number, status)
                VALUES (?, ?, ?, ?, 'AVAILABLE')
                RETURNING id, name, email, phone, license_number, status, created_at
                """, driverMapper, request.name(), request.email(), request.phone(), request.licenseNumber());
    }

    Optional<Driver> findDriver(long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("""
                    SELECT id, name, email, phone, license_number, status, created_at
                    FROM drivers WHERE id = ?
                    """, driverMapper, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    List<Driver> findAvailableDrivers() {
        return jdbc.query("""
                SELECT id, name, email, phone, license_number, status, created_at
                FROM drivers
                WHERE status = 'AVAILABLE'
                ORDER BY id
                """, driverMapper);
    }

    Optional<Driver> updateDriverStatus(long id, DriverStatus status) {
        List<Driver> drivers = jdbc.query("""
                UPDATE drivers
                SET status = ?
                WHERE id = ?
                RETURNING id, name, email, phone, license_number, status, created_at
                """, driverMapper, status.name(), id);
        return drivers.stream().findFirst();
    }

    Optional<Driver> allocateDriver() {
        List<Driver> drivers = jdbc.query("""
                UPDATE drivers
                SET status = 'BUSY'
                WHERE id = (
                    SELECT id FROM drivers
                    WHERE status = 'AVAILABLE'
                    ORDER BY id
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1
                )
                RETURNING id, name, email, phone, license_number, status, created_at
                """, driverMapper);
        return drivers.stream().findFirst();
    }

    private Passenger mapPassenger(ResultSet rs, int rowNum) throws SQLException {
        return new Passenger(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getObject("created_at", java.time.OffsetDateTime.class));
    }

    private Driver mapDriver(ResultSet rs, int rowNum) throws SQLException {
        return new Driver(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("license_number"),
                DriverStatus.valueOf(rs.getString("status")),
                rs.getObject("created_at", java.time.OffsetDateTime.class));
    }
}
