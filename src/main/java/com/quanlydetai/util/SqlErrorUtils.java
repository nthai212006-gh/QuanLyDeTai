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

                // Check for duplicate topic_code
                if (msg.contains("topic_code") || (msg.contains("Duplicate entry") && msg.contains("topics"))) {
                    return "Mã đề tài đã tồn tại trong hệ thống. Vui lòng nhập mã đề tài khác!";
                }

                // Check for max 2 supervisors trigger
                if (msg.contains("tối đa 2") || msg.contains("t?i ?a 2") || (msg.contains("2") && msg.contains("gi"))) {
                    return "Quy chế: Mỗi đề tài chỉ được hướng dẫn bởi tối đa 2 giảng viên!";
                }

                // Check for max 1 primary supervisor trigger
                if (msg.contains("duy nhất 1 giảng viên hướng dẫn chính") || msg.contains("duy nhất 1") || (msg.contains("duy") && msg.contains("1"))) {
                    return "Quy chế: Mỗi đề tài chỉ được có duy nhất 1 giảng viên hướng dẫn chính!";
                }

                if (msg.contains("Hội đồng đã bắt đầu chấm điểm")
                        || msg.contains("b???t ?????u ch???m ??i???m")) {
                    if (msg.contains("thêm") || msg.contains("th??m")) {
                        return "Hội đồng đã bắt đầu chấm điểm, không được phép thêm thành viên mới!";
                    }
                    if (msg.contains("chỉnh sửa") || msg.contains("ch???nh s???a")) {
                        return "Hội đồng đã bắt đầu chấm điểm, không được phép chỉnh sửa thông tin thành viên!";
                    }
                    if (msg.contains("xóa") || msg.contains("x??a")) {
                        return "Hội đồng đã bắt đầu chấm điểm, không được phép xóa thành viên hội đồng!";
                    }
                    return "Hội đồng đã bắt đầu chấm điểm, không được phép thay đổi thành viên!";
                }

                if (msg.contains("Chưa đủ điểm") || msg.contains("Ch??a ????? ??i???m")
                        || msg.contains("chưa hoàn tất chấm điểm") || msg.contains("Chưa thể công bố")) {
                    return "Chưa thể công bố: Chưa đủ điểm thành phần từ tất cả thành viên trong hội đồng phản biện!";
                }

                if (msg.contains("Da het han nop bao cao") || msg.contains("hết hạn nộp báo cáo")) {
                    return "Đã hết hạn nộp báo cáo tiến độ theo quy định!";
                }

                if (msg.contains("Lock wait timeout exceeded") 
                        || msg.contains("1205") 
                        || msg.contains("CannotAcquireLockException")
                        || msg.contains("LockAcquisitionException")
                        || msg.contains("PessimisticLockException")) {
                    return "Hệ thống đang bận xử lý yêu cầu song song của hội đồng, vui lòng thử lại sau giây lát!";
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
