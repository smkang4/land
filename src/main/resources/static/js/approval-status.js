/**
 * approval_m.appr_stat / appr_admin 공통 표시
 */
function formatApprStat(row) {
    if (!row || row.appr_no === undefined || row.appr_no === null || row.appr_no === '') {
        return '요청전';
    }
    var stat = String(row.appr_stat);
    var admin = row.appr_admin;
    if (stat === '3') return '완료';
    if (stat === '4') return '반려';
    if (stat === '2' && admin === 'T') return '접수대기';
    if (stat === '0' || stat === '1' || stat === '2') return '진행';
    return '요청';
}

function isCcViewerRow(row) {
    return row && row.view_role === 'CC';
}

function formatViewRole(value) {
    if (value === 'CC') {
        return '<span class="badge bg-secondary">참조</span>';
    }
    return '<span class="badge bg-primary">결재</span>';
}
