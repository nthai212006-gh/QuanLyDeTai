/**
 * ====================================================================
 * FIT THESIS PORTAL - AI ASSISTANT SCRIPTS (ai-assistant.js)
 * ====================================================================
 */

/**
 * Kiểm tra nhanh tỷ lệ trùng lặp ý tưởng khi Giảng viên tạo đề tài mới
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
            resultDiv.innerHTML = '<i class="fa-solid fa-triangle-exclamation me-2"></i> <strong>' + data.message + '</strong>';
        } else {
            resultDiv.className = 'alert alert-success py-2 small';
            resultDiv.innerHTML = '<i class="fa-solid fa-circle-check me-2"></i> <strong>' + data.message + '</strong>';
        }
    })
    .catch(error => {
        resultDiv.className = 'alert alert-danger py-2 small';
        resultDiv.innerHTML = '<i class="fa-solid fa-circle-xmark me-2"></i> Lỗi khi phân tích AI: ' + error.message;
    });
}

/**
 * Nhờ AI tư vấn & gợi ý đề tài theo sở thích của sinh viên
 */
function askAiRecommend() {
    const interestInput = document.getElementById('recInterest');
    const periodSelect = document.getElementById('recPeriodId');
    const outputDiv = document.getElementById('recOutput');

    if (!interestInput || !outputDiv) return;

    const interest = interestInput.value.trim();
    const periodId = periodSelect ? periodSelect.value : 1;

    if (!interest) {
        alert('Vui lòng nhập sở thích hoặc định hướng công nghệ của bạn!');
        interestInput.focus();
        return;
    }

    outputDiv.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2 text-primary"></i> AI đang phân tích dữ liệu đề tài đang mở trong đợt...';

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
        if (!response.ok) throw new Error('Mã lỗi: ' + response.status);
        return response.json();
    })
    .then(data => {
        outputDiv.innerHTML = data.result;
    })
    .catch(error => {
        outputDiv.innerHTML = '<div class="text-danger"><i class="fa-solid fa-circle-xmark me-1"></i> Lỗi kết nối: ' + error.message + '</div>';
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

    if (!titleInput || !outputDiv) return;

    const title = titleInput.value.trim();
    const desc = descInput ? descInput.value.trim() : '';
    const periodId = periodSelect ? periodSelect.value : 1;

    if (!title) {
        alert('Vui lòng nhập Tên đề tài!');
        titleInput.focus();
        return;
    }

    outputDiv.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2 text-danger"></i> AI đang rà quét ngân hàng đề tài...';

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
        if (!response.ok) throw new Error('Mã lỗi: ' + response.status);
        return response.json();
    })
    .then(data => {
        const badgeClass = data.isDuplicate ? 'text-danger fw-bold' : 'text-success fw-bold';
        outputDiv.innerHTML = `
            <div class="mb-2">
                <span class="fs-5 ${badgeClass}">Tỷ lệ tương đồng: ${data.score}%</span>
            </div>
            <p class="mb-0 text-secondary">${data.message}</p>
        `;
    })
    .catch(error => {
        outputDiv.innerHTML = '<div class="text-danger"><i class="fa-solid fa-circle-xmark me-1"></i> Lỗi kết nối: ' + error.message + '</div>';
    });
}

/**
 * Trích xuất bản tóm tắt nhanh báo cáo (Executive Summary)
 */
function askAiSummary() {
    const titleInput = document.getElementById('sumTitle');
    const contentInput = document.getElementById('sumContent');
    const outputDiv = document.getElementById('sumOutput');

    if (!titleInput || !contentInput || !outputDiv) return;

    const title = titleInput.value.trim();
    const content = contentInput.value.trim();

    if (!title || !content) {
        alert('Vui lòng nhập Tên đề tài và Nội dung trích đoạn báo cáo!');
        return;
    }

    outputDiv.innerHTML = '<i class="fa-solid fa-spinner fa-spin me-2 text-info"></i> AI đang phân tích và trích xuất nội dung cốt lõi...';

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
        if (!response.ok) throw new Error('Mã lỗi: ' + response.status);
        return response.json();
    })
    .then(data => {
        outputDiv.innerHTML = data.summary;
    })
    .catch(error => {
        outputDiv.innerHTML = '<div class="text-danger"><i class="fa-solid fa-circle-xmark me-1"></i> Lỗi kết nối: ' + error.message + '</div>';
    });
}
