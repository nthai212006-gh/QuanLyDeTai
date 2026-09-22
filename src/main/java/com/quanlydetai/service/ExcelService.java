package com.quanlydetai.service;

import com.quanlydetai.entity.CouncilTopic;
import com.quanlydetai.repository.CouncilTopicRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private final CouncilTopicRepository councilTopicRepository;

    public ByteArrayInputStream exportPeriodGradesToExcel(Long councilId) throws IOException {
        String[] columns = {"STT", "Mã Hội Đồng", "Mã Nhóm", "Tên Nhóm", "Tên Đề Tài", "Bộ Môn", "GVPB", "Điểm Cuối Cùng", "Kết Quả"};

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bảng Điểm Hội Đồng");

            // Header Font & Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Row for Header
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerCellStyle);
            }

            List<CouncilTopic> list = councilTopicRepository.findByCouncilId(councilId);
            int rowIdx = 1;
            for (CouncilTopic ct : list) {
                Row row = sheet.createRow(rowIdx);

                row.createCell(0).setCellValue(rowIdx);
                row.createCell(1).setCellValue(ct.getCouncil().getCouncilCode());
                row.createCell(2).setCellValue(ct.getGroup().getGroupCode());
                row.createCell(3).setCellValue(ct.getGroup().getGroupName());
                row.createCell(4).setCellValue(ct.getGroup().getTopic() != null ? ct.getGroup().getTopic().getTitle() : "Chưa có đề tài");
                row.createCell(5).setCellValue(ct.getGroup().getTopic() != null ? ct.getGroup().getTopic().getDepartment().getDeptName() : "N/A");
                row.createCell(6).setCellValue(ct.getReviewerLecturer() != null ? ct.getReviewerLecturer().getFullName() : "Chưa phân công");
                
                BigDecimal score = ct.getFinalCouncilScore();
                if (score != null) {
                    row.createCell(7).setCellValue(score.doubleValue());
                    row.createCell(8).setCellValue(score.doubleValue() >= 5.0 ? "ĐẠT" : "KHÔNG ĐẠT");
                } else {
                    row.createCell(7).setCellValue("Chưa có điểm");
                    row.createCell(8).setCellValue("Đang chấm");
                }

                rowIdx++;
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
