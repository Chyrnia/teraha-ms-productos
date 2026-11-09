package com.teraha.products.service;

import java.time.DateTimeException;
import java.time.YearMonth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.teraha.products.configs.PagingConfigs;
import com.teraha.commons.dtos.ProductMonthlyReportDTO;
import com.teraha.products.repository.ProductReportRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductReportService {

    private final ProductReportRepository reportRepository;
    private final PagingConfigs pagingConfigs;

    public Page<ProductMonthlyReportDTO> getBestSellers(
            int year,
            int month,
            int page
    ) {
        return getReport(year, month, page, true);
    }

    public Page<ProductMonthlyReportDTO> getHighestRevenue(
            int year,
            int month,
            int page
    ) {
        return getReport(year, month, page, false);
    }

    private Page<ProductMonthlyReportDTO> getReport(
            int year,
            int month,
            int page,
            boolean bestSellers
    ) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page cannot be negative"
            );
        }

        final YearMonth reportingMonth;

        try {
            reportingMonth = YearMonth.of(year, month);
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid year or month"
            );
        }

        var pageable = PageRequest.of(
                page,
                pagingConfigs.getPageSize()
        );

        if (bestSellers) {
            return reportRepository.findBestSellers(
                    reportingMonth,
                    pageable
            );
        }

        return reportRepository.findHighestRevenue(
                reportingMonth,
                pageable
        );
    }
}
