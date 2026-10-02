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
    const timelineGvpb = document.getElementById('timelineGvpbContainer');
    const timelineCouncil = document.getElementById('timelineCouncilContainer');

    if (!periodSelect || !gvpbField || !councilField) {
        return;
    }

    const type = periodSelect.value;
    if (type === 'INTERNSHIP') {
        gvpbField.style.display = 'block';
        councilField.style.display = 'none';
        if (timelineGvpb) timelineGvpb.style.display = 'block';
        if (timelineCouncil) timelineCouncil.style.display = 'none';
    } else if (type === 'GRADUATION_THESIS') {
        gvpbField.style.display = 'block';
        councilField.style.display = 'block';
        if (timelineGvpb) timelineGvpb.style.display = 'block';
        if (timelineCouncil) timelineCouncil.style.display = 'block';
    } else {
        gvpbField.style.display = 'none';
        councilField.style.display = 'none';
        if (timelineGvpb) timelineGvpb.style.display = 'none';
        if (timelineCouncil) timelineCouncil.style.display = 'none';
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
    const supervisors = btn.getAttribute('data-supervisors');

    document.getElementById('modalCouncilTopicId').value = topicId;
    document.getElementById('modalCouncilId').value = councilId;
    document.getElementById('modalTopicTitle').innerText = topicTitle;
    document.getElementById('modalGroupName').innerText = groupName;
    const modalSupervisors = document.getElementById('modalSupervisors');
    if (modalSupervisors) {
        modalSupervisors.innerText = supervisors || '';
    }

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

    // 4. Khởi tạo Flatpickr date/datetime pickers
    initFlatpickrInputs();

    // 5. Khởi tạo bộ xử lý Validation tiếng Việt & Submit Interceptor
    initFormValidationHandlers();

    // 6. Khởi tạo Live Interactive Timeline Preview Widget cho trang tạo đợt
    initPeriodTimeline();
});

/**
 * ====================================================================
 * FLATPICKR DATEPICKER & FORM VALIDATION (VIETNAMESE LOCALIZATION)
 * ====================================================================
 */

function initFlatpickrInputs() {
    if (typeof flatpickr === 'undefined') {
        return;
    }

    // Thiết lập locale tiếng Việt
    if (flatpickr.l10ns && flatpickr.l10ns.vn) {
        flatpickr.localize(flatpickr.l10ns.vn);
    }

    // 1. Khởi tạo trường Ngày & Giờ (dd/mm/yyyy HH:mm)
    document.querySelectorAll('.datetime-picker').forEach(function (input) {
        const isRequired = input.hasAttribute('required') || input.getAttribute('data-required') === 'true';
        if (isRequired) {
            input.setAttribute('data-required', 'true');
            input.removeAttribute('required'); // Tránh lỗi browser constraint validation trên input ẩn
        }

        flatpickr(input, {
            enableTime: true,
            time_24hr: true,
            dateFormat: "Y-m-d\\TH:i",
            altInput: true,
            altFormat: "d/m/Y H:i",
            altInputClass: "form-control alt-flatpickr-input",
            locale: "vn",
            allowInput: true,
            onChange: function (selectedDates, dateStr, instance) {
                if (instance.altInput) {
                    instance.altInput.classList.remove('is-invalid');
                    instance.altInput.setCustomValidity('');
                }
                triggerTimelineUpdate();
            }
        });
    });

    // 2. Khởi tạo trường Chỉ Ngày (dd/mm/yyyy)
    document.querySelectorAll('.date-picker').forEach(function (input) {
        const isRequired = input.hasAttribute('required') || input.getAttribute('data-required') === 'true';
        if (isRequired) {
            input.setAttribute('data-required', 'true');
            input.removeAttribute('required');
        }

        flatpickr(input, {
            dateFormat: "Y-m-d",
            altInput: true,
            altFormat: "d/m/Y",
            altInputClass: "form-control alt-flatpickr-input",
            locale: "vn",
            allowInput: true,
            onChange: function (selectedDates, dateStr, instance) {
                if (instance.altInput) {
                    instance.altInput.classList.remove('is-invalid');
                    instance.altInput.setCustomValidity('');
                }
                triggerTimelineUpdate();
            }
        });
    });
}

function initFormValidationHandlers() {
    // 1. Việt hóa HTML5 Constraint Validation tooltips toàn cục
    document.addEventListener('invalid', function (e) {
        const target = e.target;
        target.setCustomValidity(''); // Reset trước mỗi chu trình

        if (target.validity.valueMissing) {
            const customMsg = target.getAttribute('data-v-msg');
            if (customMsg) {
                target.setCustomValidity(customMsg);
            } else if (target.tagName === 'SELECT') {
                target.setCustomValidity('Vui lòng chọn một mục trong danh sách.');
            } else {
                target.setCustomValidity('Vui lòng điền thông tin vào trường này.');
            }
        } else if (target.validity.typeMismatch) {
            if (target.type === 'email') {
                target.setCustomValidity('Vui lòng nhập địa chỉ email hợp lệ.');
            } else if (target.type === 'url') {
                target.setCustomValidity('Vui lòng nhập một đường dẫn URL hợp lệ.');
            }
        }
    }, true);

    // Reset validity khi người dùng bắt đầu nhập hoặc thay đổi giá trị
    document.addEventListener('input', function (e) {
        e.target.setCustomValidity('');
        e.target.classList.remove('is-invalid');
    }, true);

    document.addEventListener('change', function (e) {
        e.target.setCustomValidity('');
        e.target.classList.remove('is-invalid');
    }, true);

    // 2. Submit Interceptor cho các form có chứa trường Flatpickr
    document.querySelectorAll('form').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            const requiredPickers = form.querySelectorAll('.datetime-picker[data-required="true"], .date-picker[data-required="true"]');
            let firstInvalid = null;

            requiredPickers.forEach(function (input) {
                // Nếu trường nằm trong phần tử cha đang bị ẩn thì bỏ qua
                let parent = input.closest('#gvpbField, #councilDateField');
                if (parent && window.getComputedStyle(parent).display === 'none') {
                    return;
                }

                const altInput = input._flatpickr ? input._flatpickr.altInput : null;
                if (!input.value || !input.value.trim()) {
                    if (altInput) {
                        altInput.classList.add('is-invalid');
                        const msg = input.getAttribute('data-v-msg') || 'Vui lòng chọn thời gian cho trường này.';
                        altInput.setCustomValidity(msg);
                        if (!firstInvalid) {
                            firstInvalid = altInput;
                        }
                    }
                } else if (altInput) {
                    altInput.classList.remove('is-invalid');
                    altInput.setCustomValidity('');
                }
            });

            if (firstInvalid) {
                e.preventDefault();
                firstInvalid.focus();
                firstInvalid.reportValidity();
                return false;
            }
        });
    });
}

/**
 * ====================================================================
 * LIVE INTERACTIVE TIMELINE PREVIEW WIDGET & 3-TIER VALIDATOR (ProMax)
 * ====================================================================
 */

// Module-level scope debounce timer
let timelineDebounceTimer = null;
function triggerTimelineUpdate() {
    clearTimeout(timelineDebounceTimer);
    timelineDebounceTimer = setTimeout(updatePeriodTimeline, 150);
}

/**
 * Quản lý hiển thị Alert lỗi động ở đầu form (A11y-Safe)
 */
function showPeriodFormError(message, shouldScroll = false) {
    const alertEl = document.getElementById('periodFormErrorAlert-client');
    const textEl = document.getElementById('periodFormErrorText');
    if (!alertEl || !textEl) return;

    textEl.innerText = message;
    alertEl.classList.remove('d-none');
    alertEl.classList.add('d-flex');

    if (shouldScroll) {
        alertEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
}

function hidePeriodFormError() {
    const alertEl = document.getElementById('periodFormErrorAlert-client');
    const textEl = document.getElementById('periodFormErrorText');
    if (!alertEl || !textEl) return;

    // A11y Safe: Hide khỏi DOM trước để Screen Reader ngừng quan sát live-region
    alertEl.classList.add('d-none');
    alertEl.classList.remove('d-flex');
    // Delay nhỏ (50ms) để screen reader kịp xử lý thay đổi d-none trước khi nội dung bị xóa
    setTimeout(() => {
        textEl.innerText = '';
    }, 50);
}

// NOTE: Đồng bộ 100% với PeriodService.java: validateAndSanitizePeriod()
function evaluateTimelineRules(dates) {
    const { gvStart, gvEnd, svStart, svEnd, reviewDeadline, defenseDate } = dates;

    // --- TẦNG 1: HARD BLOCKS (Lỗi Chặn - Ưu tiên cao nhất, phát hiện lỗi là early-return ngay) ---
    // Rule 1: gvEnd <= gvStart
    if (gvStart && gvEnd && gvEnd.getTime() <= gvStart.getTime()) {
        return { status: 'ERROR', fieldId: 'topicSubmissionEnd', message: 'Lỗi: Thời gian kết thúc nộp đề tài của GV phải sau thời gian bắt đầu.' };
    }
    // Rule 2: svEnd <= svStart
    if (svStart && svEnd && svEnd.getTime() <= svStart.getTime()) {
        return { status: 'ERROR', fieldId: 'studentRegistrationEnd', message: 'Lỗi: Thời gian kết thúc đăng ký của SV phải sau thời gian bắt đầu.' };
    }
    // Rule 3: Đảo ngược toàn phần (svEnd <= gvStart)
    if (gvStart && svEnd && svEnd.getTime() <= gvStart.getTime()) {
        return { status: 'ERROR', fieldId: 'studentRegistrationEnd', message: 'Lỗi: Đợt đăng ký của sinh viên không thể kết thúc trước khi giảng viên bắt đầu nộp đề tài.' };
    }
    // Rule 3.5: Ràng buộc hai giai đoạn riêng biệt (svStart < gvEnd)
    if (svStart && gvEnd && svStart.getTime() < gvEnd.getTime()) {
        return { 
            status: 'ERROR', 
            fieldId: 'studentRegistrationStart', 
            message: 'Lỗi: Thời gian sinh viên đăng ký phải bắt đầu sau khi thời gian nộp/duyệt đề tài của GV kết thúc.' 
        };
    }
    // Rule 4: reviewDeadline < svEnd (chỉ check khi GVPB hiển thị)
    if (reviewDeadline && svEnd && reviewDeadline.getTime() < svEnd.getTime()) {
        return { status: 'ERROR', fieldId: 'reviewDeadline', message: 'Lỗi: Hạn nộp điểm GVPB phải sau khi sinh viên kết thúc đợt đăng ký.' };
    }
    // Rule 5: defenseDate < reviewDeadline (so sánh theo Calendar Date 00:00:00)
    if (defenseDate && reviewDeadline) {
        const dDate = new Date(defenseDate.getTime()); dDate.setHours(0, 0, 0, 0);
        const rDate = new Date(reviewDeadline.getTime()); rDate.setHours(0, 0, 0, 0);
        if (dDate.getTime() < rDate.getTime()) {
            return { status: 'ERROR', fieldId: 'defenseDate', message: 'Lỗi: Ngày Hội đồng phải diễn ra cùng ngày hoặc sau ngày hạn chót GVPB nộp điểm.' };
        }
    }

    // --- TẦNG 2: VALID STATE (Hợp Lệ Hoàn Toàn) ---
    const hasCoreDates = gvStart && gvEnd && svStart && svEnd;
    if (hasCoreDates) {
        return { status: 'VALID', fieldId: null, message: '✓ Các mốc thời gian hoàn toàn hợp lệ, sẵn sàng khởi tạo.' };
    }

    return { status: 'IDLE', fieldId: null, message: 'Vui lòng chọn các mốc thời gian để hệ thống thẩm định tiến trình đào tạo.' };
}

function triggerInputShake(inputElement) {
    if (!inputElement) return;
    const target = inputElement._flatpickr && inputElement._flatpickr.altInput ? inputElement._flatpickr.altInput : inputElement;
    target.classList.remove('shake-error');
    void target.offsetWidth; // Force reflow
    target.classList.add('shake-error');
    target.focus();
    target.addEventListener('animationend', function () {
        target.classList.remove('shake-error');
    }, { once: true });
}

function getSelectedDate(inputElement) {
    if (!inputElement) return null;
    // Bỏ qua nếu field đang nằm trong khối bị ẩn (VD: GVPB hoặc Hội đồng khi chọn loại đợt Môn học)
    const parent = inputElement.closest('#gvpbField, #councilDateField');
    if (parent && window.getComputedStyle(parent).display === 'none') return null;

    if (inputElement._flatpickr && inputElement._flatpickr.selectedDates && inputElement._flatpickr.selectedDates.length > 0) {
        return inputElement._flatpickr.selectedDates[0];
    }
    if (inputElement.value && inputElement.value.trim()) {
        const val = inputElement.value.trim();
        const d = new Date(val);
        if (!isNaN(d.getTime())) return d;
        const parts = val.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})(?:\s+(\d{1,2}):(\d{1,2}))?$/);
        if (parts) {
            return new Date(parts[3], parts[2] - 1, parts[1], parts[4] || 0, parts[5] || 0);
        }
    }
    return null;
}

function formatVNDateTime(date, includeTime = true) {
    if (!date) return '';
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    if (!includeTime) {
        return `${day}/${month}/${year}`;
    }
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${day}/${month}/${year} ${hours}:${minutes}`;
}

function updatePeriodTimeline() {
    const gvStartEl = document.getElementById('topicSubmissionStart');
    const gvEndEl = document.getElementById('topicSubmissionEnd');
    const svStartEl = document.getElementById('studentRegistrationStart');
    const svEndEl = document.getElementById('studentRegistrationEnd');
    const gvpbEl = document.getElementById('reviewDeadline');
    const defenseEl = document.getElementById('defenseDate');

    if (!gvStartEl || !gvEndEl) return;

    const dates = {
        gvStart: getSelectedDate(gvStartEl),
        gvEnd: getSelectedDate(gvEndEl),
        svStart: getSelectedDate(svStartEl),
        svEnd: getSelectedDate(svEndEl),
        reviewDeadline: getSelectedDate(gvpbEl),
        defenseDate: getSelectedDate(defenseEl)
    };

    // 1. Cập nhật UI Chặng 1: GV Đề xuất
    const gvTimeEl = document.getElementById('timelineGvTime');
    const gvDurEl = document.getElementById('timelineGvDuration');
    if (gvTimeEl) {
        if (dates.gvStart && dates.gvEnd) {
            gvTimeEl.innerHTML = `<i class="fa-regular fa-calendar-check text-primary me-1"></i><span>${formatVNDateTime(dates.gvStart)} - ${formatVNDateTime(dates.gvEnd)}</span>`;
            const diffDays = Math.ceil((dates.gvEnd - dates.gvStart) / (1000 * 60 * 60 * 24));
            if (diffDays > 0 && gvDurEl) {
                const weeks = (diffDays / 7).toFixed(1);
                gvDurEl.style.display = 'inline-block';
                gvDurEl.innerText = `${diffDays} ngày (~${weeks} tuần)`;
            }
        } else if (dates.gvStart) {
            gvTimeEl.innerHTML = `<i class="fa-regular fa-calendar me-1"></i><span>Từ: ${formatVNDateTime(dates.gvStart)}</span>`;
            if (gvDurEl) gvDurEl.style.display = 'none';
        } else {
            gvTimeEl.innerHTML = `<i class="fa-regular fa-calendar me-1"></i><span>Chưa chọn ngày</span>`;
            if (gvDurEl) gvDurEl.style.display = 'none';
        }
    }

    // 2. Cập nhật UI Chặng 2: SV Đăng ký
    const svTimeEl = document.getElementById('timelineSvTime');
    const svDurEl = document.getElementById('timelineSvDuration');
    if (svTimeEl) {
        if (dates.svStart && dates.svEnd) {
            svTimeEl.innerHTML = `<i class="fa-regular fa-calendar-check text-success me-1"></i><span>${formatVNDateTime(dates.svStart)} - ${formatVNDateTime(dates.svEnd)}</span>`;
            const diffDays = Math.ceil((dates.svEnd - dates.svStart) / (1000 * 60 * 60 * 24));
            if (diffDays > 0 && svDurEl) {
                const weeks = (diffDays / 7).toFixed(1);
                svDurEl.style.display = 'inline-block';
                svDurEl.innerText = `${diffDays} ngày (~${weeks} tuần)`;
            }
        } else if (dates.svStart) {
            svTimeEl.innerHTML = `<i class="fa-regular fa-calendar me-1"></i><span>Từ: ${formatVNDateTime(dates.svStart)}</span>`;
            if (svDurEl) svDurEl.style.display = 'none';
        } else {
            svTimeEl.innerHTML = `<i class="fa-regular fa-calendar me-1"></i><span>Chưa chọn ngày</span>`;
            if (svDurEl) svDurEl.style.display = 'none';
        }
    }

    // 3. Cập nhật UI Chặng 3: GVPB & Hội đồng
    const gvpbTimeEl = document.getElementById('timelineGvpbTime');
    if (gvpbTimeEl) {
        if (dates.reviewDeadline) {
            gvpbTimeEl.innerHTML = `<i class="fa-solid fa-file-pen me-1 text-primary"></i><span>Hạn GVPB: <strong>${formatVNDateTime(dates.reviewDeadline)}</strong></span>`;
        } else {
            gvpbTimeEl.innerHTML = `<i class="fa-solid fa-file-pen me-1 text-muted"></i><span>Hạn GVPB: Chưa chọn</span>`;
        }
    }

    const councilTimeEl = document.getElementById('timelineCouncilTime');
    if (councilTimeEl) {
        if (dates.defenseDate) {
            councilTimeEl.innerHTML = `<i class="fa-solid fa-award me-1 text-danger"></i><span>Hội đồng: <strong>${formatVNDateTime(dates.defenseDate, false)}</strong></span>`;
        } else {
            councilTimeEl.innerHTML = `<i class="fa-solid fa-award me-1 text-muted"></i><span>Hội đồng: Chưa chọn</span>`;
        }
    }

    // 4. Tính Tổng thời gian toàn đợt
    const allDates = [dates.gvStart, dates.gvEnd, dates.svStart, dates.svEnd, dates.reviewDeadline, dates.defenseDate].filter(d => d !== null);
    const durationBadge = document.getElementById('timelineTotalDurationBadge');
    const mobileSummary = document.getElementById('mobileTimelineSummary');
    if (allDates.length >= 2) {
        const minDate = new Date(Math.min(...allDates.map(d => d.getTime())));
        const maxDate = new Date(Math.max(...allDates.map(d => d.getTime())));
        const totalDays = Math.ceil((maxDate - minDate) / (1000 * 60 * 60 * 24));
        const totalWeeks = (totalDays / 7).toFixed(1);
        const textStr = `${totalWeeks} tuần (${totalDays} ngày)`;
        if (durationBadge) durationBadge.innerText = `Tổng: ${textStr}`;
        if (mobileSummary) mobileSummary.innerText = `Dự kiến: ${textStr}`;
    } else {
        if (durationBadge) durationBadge.innerText = 'Chờ dữ liệu';
        if (mobileSummary) mobileSummary.innerText = 'Dự kiến: Chưa đủ dữ liệu';
    }

    // 5. Thẩm định 3 tầng ưu tiên
    const evaluation = evaluateTimelineRules(dates);
    const banner = document.getElementById('timelineStatusBanner');
    const mobileStatus = document.getElementById('mobileTimelineStatusBadge');

    if (banner) {
        banner.className = 'timeline-status-banner mb-3';
        if (evaluation.status === 'ERROR') {
            banner.classList.add('status-error');
            banner.innerHTML = `<i class="fa-solid fa-triangle-exclamation fs-5 text-danger mt-0.5"></i><div>${evaluation.message}</div>`;
            if (mobileStatus) {
                mobileStatus.className = 'badge bg-danger rounded-pill small';
                mobileStatus.innerText = 'Lỗi ngày';
            }
            showPeriodFormError(evaluation.message, false);
        } else if (evaluation.status === 'WARNING') {
            banner.classList.add('status-warning');
            banner.innerHTML = `<i class="fa-solid fa-circle-exclamation fs-5 text-warning mt-0.5"></i><div>${evaluation.message}</div>`;
            if (mobileStatus) {
                mobileStatus.className = 'badge bg-warning text-dark rounded-pill small';
                mobileStatus.innerText = 'Cảnh báo';
            }
            hidePeriodFormError();
        } else if (evaluation.status === 'VALID') {
            banner.classList.add('status-valid');
            banner.innerHTML = `<i class="fa-solid fa-circle-check fs-5 text-success mt-0.5"></i><div>${evaluation.message}</div>`;
            if (mobileStatus) {
                mobileStatus.className = 'badge bg-success rounded-pill small';
                mobileStatus.innerText = 'Hợp lệ';
            }
            hidePeriodFormError();
        } else {
            banner.classList.add('status-warning');
            banner.innerHTML = `<i class="fa-solid fa-circle-info fs-5 text-muted mt-0.5"></i><div>${evaluation.message}</div>`;
            if (mobileStatus) {
                mobileStatus.className = 'badge bg-secondary rounded-pill small';
                mobileStatus.innerText = 'Chờ nhập';
            }
            hidePeriodFormError();
        }
    }
}

function initPeriodTimeline() {
    const periodForm = document.getElementById('periodForm');
    if (!periodForm) return;

    // Focus management cho Server-side Error Alert khi trang reload từ Backend (A11y)
    const serverAlert = document.getElementById('periodFormErrorAlert-server');
    if (serverAlert) {
        serverAlert.focus();
    }

    // Lắng nghe sự kiện change và input trên form để cập nhật timeline widget
    periodForm.addEventListener('change', triggerTimelineUpdate);
    periodForm.addEventListener('input', triggerTimelineUpdate);

    // Bắt sự kiện submit của form để chặn Hard Block
    periodForm.addEventListener('submit', function (e) {
        const gvStartEl = document.getElementById('topicSubmissionStart');
        const gvEndEl = document.getElementById('topicSubmissionEnd');
        const svStartEl = document.getElementById('studentRegistrationStart');
        const svEndEl = document.getElementById('studentRegistrationEnd');
        const gvpbEl = document.getElementById('reviewDeadline');
        const defenseEl = document.getElementById('defenseDate');

        const dates = {
            gvStart: getSelectedDate(gvStartEl),
            gvEnd: getSelectedDate(gvEndEl),
            svStart: getSelectedDate(svStartEl),
            svEnd: getSelectedDate(svEndEl),
            reviewDeadline: getSelectedDate(gvpbEl),
            defenseDate: getSelectedDate(defenseEl)
        };

        const evaluation = evaluateTimelineRules(dates);

        if (evaluation.status === 'ERROR') {
            e.preventDefault();
            e.stopPropagation();
            if (evaluation.fieldId) {
                const targetEl = document.getElementById(evaluation.fieldId);
                if (targetEl) {
                    triggerInputShake(targetEl);
                }
            }
            showPeriodFormError(evaluation.message, true);
            updatePeriodTimeline();
            return false;
        } else {
            hidePeriodFormError();
        }
    });

    // Cập nhật ban đầu
    setTimeout(updatePeriodTimeline, 200);
}