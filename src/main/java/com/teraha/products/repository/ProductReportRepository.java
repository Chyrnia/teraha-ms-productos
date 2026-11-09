package com.teraha.products.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.teraha.commons.dtos.ProductMonthlyReportDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProductReportRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /*
     * Aggregates sales per product for the requested month.
     *
     * Revenue uses the historical unit_price recorded on each
     * sale detail, not the product's current price.
     */
    private static final String MONTHLY_SALES_CTE = """
        WITH monthly_sales AS (
            SELECT
                sd.product_id,
                SUM(sd.quantity) AS total_sold,
                SUM(sd.quantity * sd.unit_price) AS total_revenue
            FROM sale_invoice_details sd
            JOIN sale_invoices si
                ON si.id = sd.invoice_id
            WHERE si.transaction_date >= :start
              AND si.transaction_date < :end
            GROUP BY sd.product_id
        ),
        buyer_totals AS (
            SELECT
                sd.product_id,
                si.client_id,
                c.name AS buyer_name,
                SUM(sd.quantity) AS quantity_bought
            FROM sale_invoice_details sd
            JOIN sale_invoices si
                ON si.id = sd.invoice_id
            JOIN clients c
                ON c.id = si.client_id
            WHERE si.transaction_date >= :start
              AND si.transaction_date < :end
            GROUP BY
                sd.product_id,
                si.client_id,
                c.name
        ),
        ranked_buyers AS (
            SELECT
                product_id,
                buyer_name,
                quantity_bought,
                ROW_NUMBER() OVER (
                    PARTITION BY product_id
                    ORDER BY quantity_bought DESC, client_id ASC
                ) AS buyer_rank
            FROM buyer_totals
        )
        """;

    private static final String COUNT_SQL = MONTHLY_SALES_CTE + """
        SELECT COUNT(*)
        FROM monthly_sales
        """;

    private static final String SELECT_SQL = MONTHLY_SALES_CTE + """
        SELECT
            p.id AS product_id,
            p.code,
            p.description,
            p.price,
            ms.total_sold,
            ms.total_revenue,
            rb.buyer_name AS top_buyer_name,
            rb.quantity_bought AS top_buyer_quantity
        FROM monthly_sales ms
        JOIN products p
            ON p.id = ms.product_id
        LEFT JOIN ranked_buyers rb
            ON rb.product_id = p.id
           AND rb.buyer_rank = 1
        ORDER BY %s, p.id ASC
        LIMIT :limit
        OFFSET :offset
        """;

    public Page<ProductMonthlyReportDTO> findBestSellers(
            YearMonth month,
            Pageable pageable
    ) {
        return findReport(month, pageable, "ms.total_sold DESC");
    }

    public Page<ProductMonthlyReportDTO> findHighestRevenue(
            YearMonth month,
            Pageable pageable
    ) {
        return findReport(month, pageable, "ms.total_revenue DESC");
    }

    private Page<ProductMonthlyReportDTO> findReport(
            YearMonth month,
            Pageable pageable,
            String ordering
    ) {
        OffsetDateTime start = month
                .atDay(1)
                .atStartOfDay(java.time.ZoneId.of("America/Asuncion"))
                .toOffsetDateTime();

        OffsetDateTime end = month
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay(java.time.ZoneId.of("America/Asuncion"))
                .toOffsetDateTime();

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("start", start)
                .addValue("end", end);

        Long total = jdbc.queryForObject(
                COUNT_SQL,
                parameters,
                Long.class
        );

		parameters
			.addValue("limit", pageable.getPageSize())
			.addValue("offset", pageable.getOffset());

		List<ProductMonthlyReportDTO> results = jdbc.query(
				SELECT_SQL.formatted(ordering),
				parameters,
				(rs, rowNum) -> {
					ProductMonthlyReportDTO dto = new ProductMonthlyReportDTO();
					dto.setProductId(rs.getLong("product_id"));
					dto.setCode(rs.getString("code"));
					dto.setDescription(rs.getString("description"));
					dto.setPrice(rs.getBigDecimal("price"));
					dto.setTotalSold(rs.getLong("total_sold"));
					dto.setTotalRevenue(rs.getBigDecimal("total_revenue"));
					dto.setTopBuyerName(rs.getString("top_buyer_name"));
					dto.setTopBuyerQuantity(
							rs.getObject("top_buyer_quantity", Long.class)
							);

					return dto;
				});

		return new PageImpl<>(
				results,
				pageable,
				total == null ? 0 : total
				);
	}
}
