package com.quanlydetai.controller;

import com.quanlydetai.service.ExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Controller
@RequestMapping("/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelService excelService;

    @GetMapping("/export-council/{councilId}")
    @PreAuthorize("hasAnyRole('DEAN', 'HEAD_OF_DEPT', 'LECTURER', 'ADMIN')")
    public ResponseEntity<InputStreamResource> exportCouncilGrades(@PathVariable("councilId") Long councilId) throws IOException {
        ByteArrayInputStream in = excelService.exportPeriodGradesToExcel(councilId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=BangDiem_HoiDong_" + councilId + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
