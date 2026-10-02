/**
 * ====================================================================
 * FIT THESIS PORTAL - AI ASSISTANT SCRIPTS (ai-assistant.js)
 * Swiss Modernism 2.0 / Minimalist Academic AI Copilot
 * ====================================================================
 */

let copyTimeoutId = null;

/**
 * Mã hóa thực thể HTML để triệt tiêu 100% rủi ro XSS trước khi parse
 */
function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

/**
 * Format chuỗi an toàn đã escape sang định dạng thẻ học vụ
 */
function formatSafeAiMarkdown(rawText) {
    if (!rawText) return '';
    let safe = escapeHtml(rawText);

    // 1. Heading h6
    safe = safe.replace(/^### (.*$)/gim, '<div class="fw-bold text-dark fs-6 mt-3 mb-2 pb-1 border-bottom">$1</div>');

    // 2. Bold text **text**
    safe = safe.replace(/\*\*(.*?)\*\*/gim, '<strong class="text-dark">$1</strong>');

    // 3. List items - item
    safe = safe.replace(/^- (.*$)/gim, '<div class="ps-2 py-0.5">• $1</div>');

    // 4. Line breaks
    safe = safe.replace(/\n/gim, '<br>');

    return safe;
}

/**
 * Điền nhanh văn bản mẫu vào textarea và focus
 */
function fillAiPrompt(targetTextareaId, text) {
    const el = document.getElementById(targetTextareaId);
    if (el) {
        el.value = text;
        el.classList.remove('is-invalid');
        el.focus();
    }
}

/**
 * Điền nhanh cặp Tiêu đề & Mô tả cho Tab Kiểm tra trùng lặp
 */
function fillAiSimilaritySample(title, desc) {
    const titleEl = document.getElementById('simTitle');
    const descEl = document.getElementById('simDesc');
    if (titleEl) {
        titleEl.value = title;
        titleEl.classList.remove('is-invalid');
        titleEl.focus();
    }
    if (descEl) {
        descEl.value = desc;
        descEl.classList.remove('is-invalid');
    }
}

/**
 * Điền nhanh cặp Tiêu đề & Nội dung cho Tab Tóm tắt đồ án
 */
function fillAiSummarySample(title, content) {
    const titleEl = document.getElementById('sumTitle');
    const contentEl = document.getElementById('sumContent');
    if (titleEl) {
        titleEl.value = title;
        titleEl.classList.remove('is-invalid');
        titleEl.focus();
    }
    if (contentEl) {
        contentEl.value = content;
        contentEl.classList.remove('is-invalid');
    }
}

/**
 * Sao chép kết quả AI vào Clipboard với Fallback 3 tầng an toàn (Hỗ trợ LAN/HTTP)
 */
function copyAiResult(outputElementId, btnElement) {
    const outputEl = document.getElementById(outputElementId);
    if (!outputEl) return;
    const textToCopy = outputEl.innerText || outputEl.textContent;

    const onSuccess = () => {
        btnElement.innerText = "Đã sao chép ✓";
        btnElement.classList.remove("btn-outline-secondary");
        btnElement.classList.add("btn-success");

        if (copyTimeoutId) {
            clearTimeout(copyTimeoutId);
        }
        copyTimeoutId = setTimeout(() => {
            btnElement.innerText = "Sao chép";
            btnElement.classList.remove("btn-success");
            btnElement.classList.add("btn-outline-secondary");
        }, 2000);
    };

    // Tầng 1: Clipboard API chuẩn (HTTPS / localhost)
    if (navigator.clipboard && window.isSecureContext) {
        navigator.clipboard.writeText(textToCopy)
            .then(onSuccess)
            .catch(() => fallbackCopy(textToCopy, onSuccess));
    } else {
        // Tầng 2: Fallback textarea ẩn cho HTTP LAN
        fallbackCopy(textToCopy, onSuccess);
    }
}

function fallbackCopy(text, callback) {
    const textArea = document.createElement("textarea");
    textArea.value = text;
    textArea.style.position = "fixed";
    textArea.style.left = "-9999px";
    textArea.style.top = "0";
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
        const successful = document.execCommand('copy');
        if (successful) {
            callback();
        } else {
            alert("Không thể sao chép tự động. Vui lòng bôi đen và sao chép thủ công.");
        }
    } catch (err) {
        alert("Không thể sao chép tự động. Vui lòng bôi đen và sao chép thủ công.");
    }
    document.body.removeChild(textArea);
}

/**
 * Nhờ AI tư vấn & gợi ý đề tài theo sở thích của sinh viên
 */
function askAiRecommend() {
    const interestInput = document.getElementById('recInterest');
    const periodSelect = document.getElementById('recPeriodId');
    const outputDiv = document.getElementById('recOutput');
    const copyBtn = document.getElementById('btnCopyRec');

    if (!interestInput || !outputDiv) return;

    const interest = interestInput.value.trim();
    const periodId = periodSelect ? periodSelect.value : 1;

    if (!interest) {
        alert('Vui lòng nhập định hướng công nghệ hoặc chọn một gợi ý mẫu bên dưới!');
        interestInput.focus();
        return;
    }

    if (copyBtn) copyBtn.style.display = 'none';
    outputDiv.innerHTML = '<div class="p-4 text-center text-muted"><i class="fa-solid fa-spinner fa-spin me-2 text-primary"></i> Hệ thống đang phân tích ngữ nghĩa và đối chiếu cơ sở dữ liệu học vụ...</div>';

    fetch('/ai/recommend', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json; charset=utf-8'
        },
        body: JSON.stringify({
            interest: interest,
            periodId: parseInt(periodId, 10)
        })
    })
    .then(response => {
        if (!response.ok) throw new Error('Mã lỗi phản hồi: ' + response.status);
        return response.json();
    })
    .then(data => {
        outputDiv.innerHTML = formatSafeAiMarkdown(data.result);
        if (copyBtn) copyBtn.style.display = 'inline-block';
    })
    .catch(error => {
        outputDiv.innerHTML = '<div class="alert alert-danger mb-0"><i class="fa-solid fa-circle-exclamation me-1"></i> Lỗi kết nối: ' + escapeHtml(error.message) + '</div>';
    });
}

/**
 * Đối soát tính mới / trùng lặp đề tài (trên trang AI Assistant)
 */
function askAiSimilarity() {
    const titleInput = document.getElementById('simTitle');
    const descInput = document.getElementById('simDesc');
    const periodSelect = document.getElementById('simPeriodId');
    const outputDiv = document.getElementById('simOutput');
    const copyBtn = document.getElementById('btnCopySim');

    if (!titleInput || !outputDiv) return;

    const title = titleInput.value.trim();
    const desc = descInput ? descInput.value.trim() : '';
    const periodId = periodSelect ? periodSelect.value : 1;

    if (!title) {
        alert('Vui lòng nhập Tên đề tài hoặc bấm chọn mẫu thử nghiệm!');
        titleInput.focus();
        return;
    }

    if (copyBtn) copyBtn.style.display = 'none';
    outputDiv.innerHTML = '<div class="p-4 text-center text-muted"><i class="fa-solid fa-spinner fa-spin me-2 text-primary"></i> Hệ thống đang rà quét và đối soát ngân hàng đề tài các năm...</div>';

    fetch('/ai/check-similarity', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json; charset=utf-8'
        },
        body: JSON.stringify({
            title: title,
            description: desc,
            periodId: parseInt(periodId, 10)
        })
    })
    .then(response => {
        if (!response.ok) throw new Error('Mã lỗi phản hồi: ' + response.status);
        return response.json();
    })
    .then(data => {
        const score = typeof data.score === 'number' ? data.score : parseFloat(data.score) || 0;
        
        let barClass = 'bg-success';
        let textClass = 'text-success';
        let statusLabel = 'Tính mới cao (An toàn)';

        // Quy tắc Hard Flag từ Backend thắng số liệu
        if (data.isDuplicate === true || score > 60.0) {
            barClass = 'bg-danger';
            textClass = 'text-danger';
            statusLabel = 'Trùng lặp cao (Cảnh báo)';
        } else if (score >= 30.0) {
            barClass = 'bg-warning';
            textClass = 'text-warning';
            statusLabel = 'Tương đồng trung bình (Đạt chuẩn)';
        }

        const safeWidth = Math.min(Math.max(score, 0), 100);

        outputDiv.innerHTML = `
            <div class="p-3 mb-3 bg-white rounded-3 border">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span class="small fw-semibold text-secondary">TỶ LỆ TƯƠNG ĐỒNG ĐO ĐƯỢC:</span>
                    <span class="fs-6 fw-bold ${textClass}">${score}% • ${statusLabel}</span>
                </div>
                <div class="ai-progress-meter">
                    <div class="progress-bar ${barClass}" role="progressbar" style="width: ${safeWidth}%;"></div>
                </div>
            </div>
            <div class="p-3 bg-white rounded-3 border">
                <div class="small fw-semibold text-secondary mb-1">KẾT LUẬN ĐỐI SOÁT:</div>
                <div class="text-dark fw-medium">${escapeHtml(data.message)}</div>
            </div>
        `;

        if (copyBtn) copyBtn.style.display = 'inline-block';
    })
    .catch(error => {
        outputDiv.innerHTML = '<div class="alert alert-danger mb-0"><i class="fa-solid fa-circle-exclamation me-1"></i> Lỗi kết nối: ' + escapeHtml(error.message) + '</div>';
    });
}

/**
 * Trích xuất bản tóm tắt nhanh báo cáo (Executive Summary)
 */
function askAiSummary() {
    const titleInput = document.getElementById('sumTitle');
    const contentInput = document.getElementById('sumContent');
    const outputDiv = document.getElementById('sumOutput');
    const copyBtn = document.getElementById('btnCopySum');

    if (!titleInput || !contentInput || !outputDiv) return;

    const title = titleInput.value.trim();
    const content = contentInput.value.trim();

    if (!title || !content) {
        alert('Vui lòng nhập Tên đề tài và Nội dung trích đoạn báo cáo!');
        return;
    }

    if (copyBtn) copyBtn.style.display = 'none';
    outputDiv.innerHTML = '<div class="p-4 text-center text-muted"><i class="fa-solid fa-spinner fa-spin me-2 text-primary"></i> AI đang phân tích và trích xuất nội dung cốt lõi của đồ án...</div>';

    fetch('/ai/summarize', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json; charset=utf-8'
        },
        body: JSON.stringify({
            title: title,
            content: content
        })
    })
    .then(response => {
        if (!response.ok) throw new Error('Mã lỗi phản hồi: ' + response.status);
        return response.json();
    })
    .then(data => {
        outputDiv.innerHTML = formatSafeAiMarkdown(data.summary);
        if (copyBtn) copyBtn.style.display = 'inline-block';
    })
    .catch(error => {
        outputDiv.innerHTML = '<div class="alert alert-danger mb-0"><i class="fa-solid fa-circle-exclamation me-1"></i> Lỗi kết nối: ' + escapeHtml(error.message) + '</div>';
    });
}

/**
 * Kiểm tra nhanh tỷ lệ trùng lặp ý tưởng khi Giảng viên tạo đề tài mới (Modal)
 */
function checkSimilarityWithAI() {
    const titleInput = document.getElementById('topicTitle');
    const descInput = document.getElementById('topicDesc');
    const periodSelect = document.getElementById('periodSelect');
    const resultDiv = document.getElementById('aiResultDiv');

    if (!titleInput || !resultDiv) return;

    const title = titleInput.value.trim();
    const desc = descInput ? descInput.value.trim() : '';
    const periodId = periodSelect ? periodSelect.value : 1;

    if (!title) {
        alert('Vui lòng nhập Tên đề tài trước khi kiểm tra!');
        titleInput.focus();
        return;
    }

    resultDiv.className = 'alert alert-info py-2 small';
    resultDiv.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2"></i> AI đang phân tích dữ liệu và đối soát kho đề tài...';
    resultDiv.classList.remove('d-none');

    fetch('/ai/check-similarity', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json; charset=utf-8'
        },
        body: JSON.stringify({
            title: title,
            description: desc,
            periodId: parseInt(periodId, 10)
        })
    })
    .then(response => {
        if (!response.ok) throw new Error('Mã lỗi mạng: ' + response.status);
        return response.json();
    })
    .then(data => {
        if (data.isDuplicate) {
            resultDiv.className = 'alert alert-warning py-2 small';
            resultDiv.innerHTML = '<i class="fa-solid fa-triangle-exclamation me-2"></i> <strong>' + escapeHtml(data.message) + '</strong>';
        } else {
            resultDiv.className = 'alert alert-success py-2 small';
            resultDiv.innerHTML = '<i class="fa-solid fa-circle-check me-2"></i> <strong>' + escapeHtml(data.message) + '</strong>';
        }
    })
    .catch(error => {
        resultDiv.className = 'alert alert-danger py-2 small';
        resultDiv.innerHTML = '<i class="fa-solid fa-circle-xmark me-2"></i> Lỗi khi phân tích AI: ' + escapeHtml(error.message);
    });
}
