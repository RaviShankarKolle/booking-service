package com.library.fine.fine;



import org.springframework.jdbc.core.RowMapper;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.springframework.stereotype.Repository;



import java.math.BigDecimal;

import java.sql.ResultSet;

import java.sql.SQLException;

import java.time.Instant;

import java.time.LocalDate;

import java.util.List;

import java.util.Optional;



@Repository

public class FineRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;



    public FineRepository(NamedParameterJdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;

    }


    public Optional<FineRecord> findByBorrowId(Long borrowId) {

        String sql = """

                SELECT id, borrow_id, user_id, book_id, amount, reason, status, last_calculated_date, updated_at

                FROM fines

                WHERE borrow_id = :borrowId

                """;

        List<FineRecord> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("borrowId", borrowId), new FineRowMapper());

        return rows.stream().findFirst();

    }


    public Optional<FineRecord> findById(Long fineId) {

        String sql = """

                SELECT id, borrow_id, user_id, book_id, amount, reason, status, last_calculated_date, updated_at

                FROM fines

                WHERE id = :id

                """;
        List<FineRecord> rows = jdbcTemplate.query(sql, new MapSqlParameterSource("id", fineId), new FineRowMapper());

        return rows.stream().findFirst();

    }


    public Long create(Long borrowId, Long userId, Long bookId, BigDecimal amount, String reason, String status, LocalDate calculatedDate) {

        String sql = """
 INSERT INTO fines(borrow_id, user_id, book_id, amount, reason, status, last_calculated_date)

                VALUES(:borrowId, :userId, :bookId, :amount, :reason, :status, :calculatedDate)

                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()

                .addValue("borrowId", borrowId)

                .addValue("userId", userId)

                .addValue("bookId", bookId)

                .addValue("amount", amount)

                .addValue("reason", reason)

                .addValue("status", status)

                .addValue("calculatedDate", calculatedDate));

        String idSql = "SELECT id FROM fines WHERE borrow_id = :borrowId ORDER BY id DESC LIMIT 1";

        Long id = jdbcTemplate.queryForObject(idSql, new MapSqlParameterSource("borrowId", borrowId), Long.class);

        return id == null ? 0L : id;

    }



    public void updateFine(Long fineId, BigDecimal amount, String reason, String status, LocalDate calculatedDate) {

        String sql = """

                UPDATE fines

                SET amount = :amount, reason = :reason, status = :status, last_calculated_date = :calculatedDate, updated_at = CURRENT_TIMESTAMP

                WHERE id = :id

                """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()

                .addValue("id", fineId)

                .addValue("amount", amount)

                .addValue("reason", reason)

                .addValue("status", status)

                .addValue("calculatedDate", calculatedDate));

    }



    public record FineRecord(

            Long id,

            Long borrowId,

            Long userId,

            Long bookId,

            BigDecimal amount,

            String reason,

            String status,

            LocalDate lastCalculatedDate,

            Instant updatedAt

    ) {}



    private static class FineRowMapper implements RowMapper<FineRecord> {

        @Override

        public FineRecord mapRow(ResultSet rs, int rowNum) throws SQLException {

            return new FineRecord(

                    rs.getLong("id"),

                    rs.getLong("borrow_id"),

                    rs.getLong("user_id"),

                    rs.getLong("book_id"),

                    rs.getBigDecimal("amount"),

                    rs.getString("reason"),

                    rs.getString("status"),

                    rs.getDate("last_calculated_date").toLocalDate(),

                    rs.getTimestamp("updated_at").toInstant()

            );

        }

    }

}

