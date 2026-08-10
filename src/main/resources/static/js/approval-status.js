/**
 * approval_m.appr_stat / appr_admin 공통 표시
 */
function formatApprStat(row) {
    if (!row || row.appr_no === undefined || row.appr_no === null || row.appr_no === '') {
        return '<span class="list-status list-status--idle">요청전</span>';
    }
    var stat = String(row.appr_stat);
    var admin = row.appr_admin;
    var label = '요청';
    var cls = 'list-status--idle';

    if (stat === '3') {
        label = '완료';
        cls = 'list-status--ok';
    } else if (stat === '4') {
        label = '반려';
        cls = 'list-status--reject';
    } else if (stat === '2' && admin === 'T') {
        label = '접수대기';
        cls = 'list-status--wait';
    } else if (stat === '0' || stat === '1' || stat === '2') {
        label = '진행';
        cls = 'list-status--progress';
    }

    return '<span class="list-status ' + cls + '">' + label + '</span>';
}

function isCcViewerRow(row) {
    return row && row.view_role === 'CC';
}

function formatViewRole(value) {
    if (value === 'CC') {
        return '<span class="list-status list-status--idle">참조</span>';
    }
    return '<span class="list-status list-status--progress">결재</span>';
}
