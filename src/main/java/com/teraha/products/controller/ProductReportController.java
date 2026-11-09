package com.teraha.products.controller;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.teraha.commons.dtos.ProductMonthlyReportDTO;
import com.teraha.products.service.ProductReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product Reports", description = "Monthly product sales reports")
@Slf4j
@RestController
@RequestMapping(
        path = "/reports/products",
        produces = "application/json"
)
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProductReportController {

    private final ProductReportService reportService;

    @PreAuthorize("hasAnyRole('SELLER','MANAGER','SYSADMIN')")
    @GetMapping("/best-sellers")
    public Page<ProductMonthlyReportDTO> getBestSellers(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "0") int page
    ) {
        log.info(
                "Requesting best-selling products: year={} month={} page={}",
                year, month, page
        );

        return reportService.getBestSellers(year, month, page);
    }

    @PreAuthorize("hasAnyRole('SELLER','MANAGER','SYSADMIN')")
    @GetMapping("/highest-revenue")
    public Page<ProductMonthlyReportDTO> getHighestRevenue(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "0") int page
    ) {
        log.info(
                "Requesting highest-revenue products: year={} month={} page={}",
                year, month, page
        );

        return reportService.getHighestRevenue(year, month, page);
    }
}
