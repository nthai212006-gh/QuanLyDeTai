package com.quanlydetai.util;

public class SqlErrorUtils {

    private SqlErrorUtils() {
        // utility class
    }

    public static String extractFriendlyMessage(Throwable ex) {
        if (ex == null) {
            return "Đã xảy ra lỗi không xác định!";
        }

        Throwable current = ex;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null) {
                // Check for group size limit trigger (both clean UTF-8 and database dump representation)
                if (msg.contains("Nhóm sinh viên không được vượt quá tối đa 3 thành viên")
                        || msg.contains("3 th??nh vi??n")
                        || (msg.contains("3") && (msg.contains("thành viên") || msg.contains("th??nh vi??n")))) {
                    return "Quy định đề án: Nhóm sinh viên không được vượt quá tối đa 3 thành viên!";
                }

                // Check for submission by leader only trigger
                if (msg.contains("Việc nộp báo cáo đồ án chỉ được phép thực hiện bởi Nhóm trưởng")
                        || msg.contains("nh??m tr?????ng")
                        || msg.contains("nhóm trưởng")
                        || msg.contains("b??o c??o")) {
                    return "Lỗi phân quyền: Việc nộp báo cáo đồ án chỉ được phép thực hiện bởi Nhóm trưởng!";
                }

                // Check for supervisor grading trigger
                if (msg.contains("Giảng viên không được phép chấm đề tài")
                        || msg.contains("ch??nh m??nh")
                        || msg.contains("h?????ng d???n")
                        || (msg.contains("chấm") && msg.contains("hướng dẫn"))) {
                    return "Lỗi vi phạm quy chế: Giảng viên không được phép chấm đề tài do chính mình đang hướng dẫn!";
                }

                if (msg.contains("Giảng viên không được phép chỉnh sửa điểm")) {
                    return "Lỗi vi phạm quy chế: Giảng viên không được phép chỉnh sửa điểm đề tài mình đang hướng dẫn!";
                }

                if (msg.contains("Da het han nop bao cao") || msg.contains("hết hạn nộp báo cáo")) {
                    return "Đã hết hạn nộp báo cáo tiến độ theo quy định!";
                }

                // Extract SQLSTATE 45000 custom text if enclosed
                int idx = msg.indexOf("MESSAGE_TEXT = '");
                if (idx != -1) {
                    int end = msg.indexOf("'", idx + 16);
                    if (end != -1) {
                        return msg.substring(idx + 16, end);
                    }
                }
            }
            current = current.getCause();
        }

        return ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi trong quá trình xử lý dữ liệu!";
    }
}
