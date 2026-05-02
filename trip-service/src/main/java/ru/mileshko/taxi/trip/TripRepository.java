package ru.mileshko.taxi.trip;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
class TripRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<Trip> mapper = this::mapTrip;

    TripRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    Trip create(long passengerId, long driverId, String origin, String destination,
                BigDecimal distanceKm, BigDecimal price) {
        return jdbc.queryForObject("""
                INSERT INTO trips(passenger_id, driver_id, status, origin, destination, distance_km, price)
                VALUES (?, ?, 'DRIVER_ASSIGNED', ?, ?, ?, ?)
                RETURNING id, passenger_id, driver_id, status, origin, destination, distance_km, price, rating, created_at, updated_at
                """, mapper, passengerId, driverId, origin, destination, distanceKm, price);
    }

    Optional<Trip> find(long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("""
                    SELECT id, passenger_id, driver_id, status, origin, destination, distance_km, price, rating, created_at, updated_at
                    FROM trips WHERE id = ?
                    """, mapper, id));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    List<Trip> findByPassenger(long passengerId) {
        return jdbc.query("""
                SELECT id, passenger_id, driver_id, status, origin, destination, distance_km, price, rating, created_at, updated_at
                FROM trips WHERE passenger_id = ?
                ORDER BY created_at DESC
                """, mapper, passengerId);
    }

    Optional<Trip> updateStatus(long id, TripStatus status) {
        List<Trip> trips = jdbc.query("""
                UPDATE trips
                SET status = ?, updated_at = now()
                WHERE id = ?
                RETURNING id, passenger_id, driver_id, status, origin, destination, distance_km, price, rating, created_at, updated_at
                """, mapper, status.name(), id);
        return trips.stream().findFirst();
    }

    Optional<Trip> rate(long id, int rating) {
        List<Trip> trips = jdbc.query("""
                UPDATE trips
                SET rating = ?, updated_at = now()
                WHERE id = ? AND status = 'COMPLETED'
                RETURNING id, passenger_id, driver_id, status, origin, destination, distance_km, price, rating, created_at, updated_at
                """, mapper, rating, id);
        return trips.stream().findFirst();
    }

    TripStatistics statistics(LocalDate date) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS trips_count, COALESCE(AVG(price), 0) AS average_price
                FROM trips
                WHERE created_at >= ?::date AND created_at < (?::date + INTERVAL '1 day')
                """, (rs, rowNum) -> new TripStatistics(
                date,
                rs.getLong("trips_count"),
                rs.getBigDecimal("average_price")), date, date);
    }

    private Trip mapTrip(ResultSet rs, int rowNum) throws SQLException {
        return new Trip(
                rs.getLong("id"),
                rs.getLong("passenger_id"),
                (Long) rs.getObject("driver_id"),
                TripStatus.valueOf(rs.getString("status")),
                rs.getString("origin"),
                rs.getString("destination"),
                rs.getBigDecimal("distance_km"),
                rs.getBigDecimal("price"),
                (Integer) rs.getObject("rating"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class));
    }
}
