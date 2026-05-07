package ru.mileshko.taxi.notification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
class NotificationRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<NotificationTask> mapper = this::mapTask;

    NotificationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    NotificationTask create(CreateNotificationRequest request) {
        return jdbc.queryForObject("""
                INSERT INTO notification_tasks(trip_id, recipient_type, recipient_id, message, status)
                VALUES (?, ?, ?, ?, 'PENDING')
                RETURNING id, trip_id, recipient_type, recipient_id, message, status, attempts, created_at, updated_at
                """, mapper, request.tripId(), request.recipientType(), request.recipientId(), request.message());
    }

    List<NotificationTask> findByTrip(long tripId) {
        return jdbc.query("""
                SELECT id, trip_id, recipient_type, recipient_id, message, status, attempts, created_at, updated_at
                FROM notification_tasks
                WHERE trip_id = ?
                ORDER BY created_at
                """, mapper, tripId);
    }

    List<NotificationTask> findByRecipient(String recipientType, long recipientId) {
        return jdbc.query("""
                SELECT id, trip_id, recipient_type, recipient_id, message, status, attempts, created_at, updated_at
                FROM notification_tasks
                WHERE recipient_type = ? AND recipient_id = ?
                ORDER BY created_at DESC
                """, mapper, recipientType, recipientId);
    }

    Optional<NotificationTask> claimNext() {
        List<NotificationTask> tasks = jdbc.query("""
                UPDATE notification_tasks
                SET status = 'PROCESSING',
                    attempts = attempts + 1,
                    locked_at = now(),
                    updated_at = now()
                WHERE id = (
                    SELECT id FROM notification_tasks
                    WHERE status = 'PENDING' AND attempts < 3
                    ORDER BY created_at
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1
                )
                RETURNING id, trip_id, recipient_type, recipient_id, message, status, attempts, created_at, updated_at
                """, mapper);
        return tasks.stream().findFirst();
    }

    void markSent(long id) {
        jdbc.update("""
                UPDATE notification_tasks
                SET status = 'SENT', updated_at = now()
                WHERE id = ?
                """, id);
    }

    void markFailedOrRetry(long id, int attempts) {
        String status = attempts >= 3 ? "FAILED" : "PENDING";
        jdbc.update("""
                UPDATE notification_tasks
                SET status = ?, updated_at = now()
                WHERE id = ?
                """, status, id);
    }

    private NotificationTask mapTask(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationTask(
                rs.getLong("id"),
                rs.getLong("trip_id"),
                RecipientType.valueOf(rs.getString("recipient_type")),
                rs.getLong("recipient_id"),
                rs.getString("message"),
                NotificationStatus.valueOf(rs.getString("status")),
                rs.getInt("attempts"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class));
    }
}
