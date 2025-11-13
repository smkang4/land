// 기안 상태 표시 유틸리티
const DraftStatus = {
    DISPLAY: {
        '1': { text: '작성', class: 'badge bg-primary', icon: 'bi-pencil' },
        '2': { text: '업로드', class: 'badge bg-warning', icon: 'bi-cloud-upload' },
        '3': { text: '완료', class: 'badge bg-success', icon: 'bi-check-circle' }
    },
    
    // 상태 표시 HTML 생성
    getStatusHtml: function(status) {
        const display = this.DISPLAY[status] || this.DISPLAY['1'];
        return `<span class="${display.class}">
                    <i class="bi ${display.icon}"></i> ${display.text}
                </span>`;
    },
    
    // 그리드에서 사용할 포맷터
    statusFormatter: function(e) {
        return DraftStatus.getStatusHtml(e.value);
    }
};

// 그리드 컬럼 정의에 추가
const statusColumn = {
    header: '상태', 
    name: 'status', 
    width: 80, 
    align: 'center', 
    sortable: true,
    formatter: DraftStatus.statusFormatter
};




















