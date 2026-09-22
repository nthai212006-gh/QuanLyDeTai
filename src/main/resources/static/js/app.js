/**
 * ====================================================================
 * FIT THESIS PORTAL - MAIN APPLICATION SCRIPTS (app.js)
 * ====================================================================
 */

/**
 * Điều khiển ẩn/hiển thị các mốc thời gian đặc thù theo loại đợt đăng ký
 * (Áp dụng cho trang tạo đợt đăng ký /periods/create)
 */
function toggleConditionalFields() {
    const periodSelect = document.getElementById('periodTypeSelect');
    const gvpbField = document.getElementById('gvpbField');
    const councilField = document.getElementById('councilDateField');

    if (!periodSelect || !gvpbField || !councilField) {
        return;
    }

    const type = periodSelect.value;
    if (type === 'INTERNSHIP') {
        gvpbField.style.display = 'block';
        councilField.style.display = 'none';
    } else if (type === 'GRADUATION_THESIS') {
        gvpbField.style.display = 'block';
        councilField.style.display = 'block';
    } else {
        gvpbField.style.display = 'none';
        councilField.style.display = 'none';
    }
}

/**
 * Mở modal chấm điểm đề tài tại Hội đồng
 * @param {HTMLElement} btn Nút Chấm Điểm được click
 */
function openGradingModal(btn) {
    const topicId = btn.getAttribute('data-council-topic-id');
    const councilId = btn.getAttribute('data-council-id');
    const topicTitle = btn.getAttribute('data-topic-title');
    const groupName = btn.getAttribute('data-group-name');

    document.getElementById('modalCouncilTopicId').value = topicId;
    document.getElementById('modalCouncilId').value = councilId;
    document.getElementById('modalTopicTitle').innerText = topicTitle;
    document.getElementById('modalGroupName').innerText = groupName;

    // Reset fields
    document.getElementById('gradeC1').value = '';
    document.getElementById('gradeC2').value = '';
    document.getElementById('gradeC3').value = '';
    document.getElementById('gradeComments').value = '';
    calculateLiveGrade();

    const modalElement = document.getElementById('gradeTopicModal');
    if (modalElement) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        modal.show();
    }
}

/**
 * Tự động tính nhẩm điểm tổng kết (30% + 40% + 30%) hiển thị trực tiếp cho GV
 */
function calculateLiveGrade() {
    const c1 = parseFloat(document.getElementById('gradeC1').value) || 0;
    const c2 = parseFloat(document.getElementById('gradeC2').value) || 0;
    const c3 = parseFloat(document.getElementById('gradeC3').value) || 0;
    const liveScoreEl = document.getElementById('liveScore');

    if (liveScoreEl) {
        const total = (c1 * 0.3 + c2 * 0.4 + c3 * 0.3).toFixed(2);
        liveScoreEl.innerText = total;
        // Điểm nhấn Motion: Animation đếm điểm nhẹ
        liveScoreEl.classList.add('score-updated');
        setTimeout(() => liveScoreEl.classList.remove('score-updated'), 300);
    }
}

/**
 * ====================================================================
 * SIDEBAR NAVIGATION & LAYOUT CONTROLS
 * ====================================================================
 */

function toggleSidebar() {
    const isMobile = window.innerWidth < 992;
    if (isMobile) {
        document.body.classList.toggle('sidebar-mobile-open');
    } else {
        document.body.classList.toggle('sidebar-collapsed');
        const isCollapsed = document.body.classList.contains('sidebar-collapsed');
        localStorage.setItem('sidebarCollapsed', isCollapsed ? 'true' : 'false');
    }
}

// Khởi chạy khi DOM đã sẵn sàng
document.addEventListener('DOMContentLoaded', function () {
    // 1. Phục hồi trạng thái Sidebar từ localStorage (Desktop)
    if (window.innerWidth >= 992) {
        const savedState = localStorage.getItem('sidebarCollapsed');
        if (savedState === 'true') {
            document.body.classList.add('sidebar-collapsed');
        }
    }

    // 2. Tự động đóng sidebar mobile khi click vào backdrop
    const backdrop = document.getElementById('sidebar-backdrop');
    if (backdrop) {
        backdrop.addEventListener('click', function () {
            document.body.classList.remove('sidebar-mobile-open');
        });
    }

    // 3. Tự động kiểm tra toggle fields nếu đang ở trang tạo đợt
    const periodSelect = document.getElementById('periodTypeSelect');
    if (periodSelect) {
        toggleConditionalFields();
        periodSelect.addEventListener('change', toggleConditionalFields);
    }
});