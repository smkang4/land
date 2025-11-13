/**
 * 그룹웨어 결재문서 생성 API
 * 흐름)
 * 1. 프로젝트 내 결재문서 데이터
 */

(function($) {
    'use strict';

    // 전역 설정
    const APPROVAL_CONFIG = {
        API_BASE_URL: '/api/approval',
        GROUPWARE_URL: 'http://tg.dage.co.kr/_ERPR/Public/ERPRLogin.aspx',
        ERP_URL: 'http://derp.dage.co.kr/DAGE/unicon_ccms/ERPhtml01.jsp'
    };

    /**
     * 메인 클래스
     */
    class ApprovalDocumentGenerator {
        constructor(options = {}) {
            this.config = { ...APPROVAL_CONFIG, ...options };
        }

        /**
         * 결재문서 생성 및 그룹웨어/ERP 전송 (통합 메서드)
         * @param {Object} params - 파라미터 객체
         * @param {number} params.draftId - 기안서 ID
         * @param {string} params.eaId - EA ID
         * @param {string} params.userId - 사용자 ID
         * @param {string} params.projName - 프로젝트명
         * @param {string} params.gwCode - 그룹웨어 코드
         * @param {string} params.mstSeq - 마스터 시퀀스
         * @param {string} params.empNo - 사원번호
         * @param {string} params.userNo - 사용자번호
         * @param {string} params.projCode - 프로젝트 코드
         * @param {Function} params.onSuccess - 성공 콜백
         * @param {Function} params.onError - 에러 콜백
         */
        generateAndSendToBothSystems(params) {
            const {
                draftId,
                eaId,
                userId,
                projName,
                gwCode,
                mstSeq,
                empNo,
                userNo,
                userName,
                projCode,
                onSuccess,
                onError
            } = params;

            if (!draftId) {
                this.handleError('기안서 ID가 필요합니다.', onError);
                return;
            }

            // 1. HTML 생성 (클라이언트 데이터와 함께)
            this.generateDocumentHTML(draftId, {
                eaId: eaId,
                projName,
                gwCode,
                mstSeq,
                empNo,
                userNo,
                userName,
                projCode,
                attachmentList: params.attachmentList || '' // 첨부파일 리스트 전달
            })
            .then(response => {
                if (response.success) {
                    // 2. 그룹웨어 및 ERP로 전송
                    this.sendToBothSystems({
                        eaId: eaId,
                        requestName: `숙소 임대차 계약의 건 -${projName}`,
                        userId: userId,
                        empNo: empNo,
                        userNo: userNo,
                        userName: userName,
                        projCode: projCode,
                        projName: projName,
                        deptId: gwCode,
                        mstSeq: mstSeq,
                        html: response.html,
                        eDocCode: response.eDocCode,
                        eDocName: response.eDocName,
                        // 거래처 정보 추가
                        existingCustCode: params.existingCustCode,
                        custName: params.custName,
                        bizNo: params.bizNo,
                        bossName: params.bossName,
                        tradeCls: params.tradeCls,
                        bizCond: params.bizCond,
                        bizKnd: params.bizKnd,
                        zipCode: params.zipCode,
                        addr1: params.addr1,
                        addr2: params.addr2,
                        telNo: params.telNo,
                        headFax: params.headFax,
                        bankMainCode: params.bankMainCode,
                        bankCode: params.bankCode,
                        custAccNo: params.custAccNo,
                        elctTag: params.elctTag,
                        cOwner: params.cOwner,
                        remark: params.remark,
                        taxCls: params.taxCls,
                        representCustCode: params.representCustCode,
                        sBankNo: params.sBankNo,
                        regCls: params.regCls,
                        onSuccess,
                        onError
                    });
                } else {
                    throw new Error(response.message || '결재문서 HTML 생성에 실패했습니다.');
                }
            })
            .catch(error => {
                this.handleError(error.message, onError);
            });
        }

        /**
         * 결재문서 HTML 생성
         * @param {number} draftId - 기안서 ID
         * @param {Object} clientData - 클라이언트에서 수집한 데이터
         * @returns {Promise} HTML 생성 결과
         */
        generateDocumentHTML(draftId, clientData) {
            return $.ajax({
                url: `${this.config.API_BASE_URL}/generate-document`,
                type: 'GET',
                data: { 
                    draftId: draftId,
                    projName: clientData.projName,
                    gwCode: clientData.gwCode,
                    mstSeq: clientData.mstSeq,
                    empNo: clientData.empNo,
                    userNo: clientData.userNo,
                    projCode: clientData.projCode,
                    eaId: clientData.eaId,
                    attachmentList: clientData.attachmentList || '' // 첨부파일 리스트 추가
                },
                dataType: 'json'
            });
        }

        /**
         * 그룹웨어 및 ERP로 결재문서 전송 (두 시스템 모두)
         * @param {Object} params - 전송 파라미터
         */
        sendToBothSystems(params) {
            const {
                eaId,
                requestName,
                userId,
                empNo,
                userNo,
                userName,
                projCode,
                projName,
                deptId,
                mstSeq,
                html,
                eDocCode,
                eDocName,
                // 거래처 정보 추가
                existingCustCode,
                custName,
                bizNo,
                bossName,
                tradeCls,
                bizCond,
                bizKnd,
                zipCode,
                addr1,
                addr2,
                telNo,
                headFax,
                bankMainCode,
                bankCode,
                custAccNo,
                elctTag,
                cOwner,
                remark,
                taxCls,
                representCustCode,
                sBankNo,
                regCls,
                onSuccess,
                onError
            } = params;

            // 부서코드 검증 및 디버깅
            console.log('=== deptId 디버깅 ===');
            console.log('deptId 원본 값:', deptId);
            console.log('deptId 타입:', typeof deptId);
            console.log('deptId 길이:', deptId ? deptId.length : 'null');
            console.log('deptId 앞자리 0 포함:', deptId);
            
            if (!deptId || deptId.trim() === "") {
                this.handleError("전자결재 기안 부서/현장 정보를 찾을 수 없습니다.\n전산팀에 문의바랍니다.", onError);
                return;
            }

            // 1. 그룹웨어 시스템으로 전송
            this.sendToGroupware(params);
            
            // 2. ERP 시스템으로 전송 (ERP 완료 후 콜백 호출)
            this.sendToERP(params, onSuccess, onError);
        }

        /**
         * 그룹웨어 시스템으로 전송
         * @param {Object} params - 전송 파라미터
         */
        sendToGroupware(params) {
            const {
                eaId,
                requestName,
                userId,
                empNo,
                userNo,
                userName,
                projCode,
                projName,
                deptId,
                mstSeq,
                eDocName,
                html
            } = params;

            // 그룹웨어 전송 시에는 HTML을 Base64로 인코딩하지 않고 특수문자만 처리
            const sanitizedHtml = this.sanitizeHtml(html);
            
            // 폼 데이터 생성
            const formData = new FormData();
            const fields = {
                'window': '1',
                'EAID': eaId,
                'ERPLinkType': 'html',
                'lang': 'ko',
                'UserID': userId,
                'Pwd': '',
                'NextUrl': `/_EAPP/EADocumentWrite.aspx?FormID=${eaId}`,
                'ErpDocTitle': requestName,
                'htmltag': sanitizedHtml, // 특수문자만 처리된 HTML (Base64 인코딩 없음)
                'gw_num': 'DAGE',
                'gw_num2': mstSeq,
                'gw_num3': '1',
                'gw_num4': eDocName,
                'gw_num5': '',
                'gw_num6': '',
                'gw_num7': '',
                'gw_num8': '',
                'gw_num9': '',
                'gw_num10': '',
                'DeptId': deptId
            };
            
            // 폼 데이터에 필드 추가
            for (const [name, value] of Object.entries(fields)) {
                formData.append(name, value);
            }

            // 새 창 열기
            // A4 크기에 맞게 팝업 크기 확대 (A4: 210mm x 297mm, 96 DPI 기준)
            const newWindow = window.open('', '_blank', 'width=1000,height=1400,scrollbars=yes,resizable=yes,menubar=no,toolbar=no,location=no,status=no');
            
            if (newWindow) {
                // 새 창에 폼 생성 및 제출
                newWindow.document.write(`
                    <html>
                    <head><title>결재문서 전송 중...</title></head>
                    <body>
                        <form method="post" action="${this.config.GROUPWARE_URL}">
                            ${Object.entries(fields).map(([name, value]) => 
                                `<input type="hidden" name="${name}" value="${value}">`
                            ).join('')}
                        </form>
                        <script>
                            document.forms[0].submit();
                        </script>
                    </body>
                    </html>
                `);
                newWindow.document.close();
            } else {
                console.error('팝업이 차단되었습니다. 팝업 차단을 해제해주세요.');
            }
        }

        /**
         * HTML 내용의 특수문자 처리
         * @param {string} html HTML 내용
         * @returns {string} 특수문자가 처리된 HTML
         */
        sanitizeHtml(html) {
            if (!html) return '';
            
            return html
                .replace(/&/g, '&amp;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;')
                .replace(/\n/g, '<br>')
                .replace(/\r\n/g, '<br>')
                .replace(/\r/g, '<br>');
        }

        /**
         * ERP 시스템으로 전송 (프로시저 호출) -> 초기 결재 정보 insert
         * @param {Object} params - 전송 파라미터
         * @param {Function} onSuccess - 성공 콜백
         * @param {Function} onError - 에러 콜백
         */
        sendToERP(params, onSuccess, onError) {
            const {
                eaId,
                requestName,
                userId,
                empNo,
                userNo,
                userName,
                projCode,
                projName,
                deptId,
                mstSeq,
                html,
                eDocCode,
                eDocName,
                existingCustCode,
                custName,
                bizNo,
                bossName,
                tradeCls,
                bizCond,
                bizKnd,
                zipCode,
                addr1,
                addr2,
                telNo,
                headFax,
                bankMainCode,
                bankCode,
                custAccNo,
                elctTag,
                cOwner,
                remark,
                taxCls,
                representCustCode,
                sBankNo,
                regCls
            } = params;

            // HTML 내용의 특수문자 처리 및 Base64 인코딩
            const sanitizedHtml = this.sanitizeHtml(html);
            const encodedHtml = btoa(unescape(encodeURIComponent(sanitizedHtml)));

            // ERP 프로시저 호출을 위한 데이터
            const erpData = {
                html: encodedHtml, // Base64로 인코딩된 HTML
                mstSeq: mstSeq,
                subSeq: 1, // 기본값 select로 가져와야함
                crtUserNo: userNo,
                makeProj: projCode, // proj_code로 사용
                makeDt: new Date().toISOString().split('T')[0], // YYYY-MM-DD 형식
                makeSeq: null, // 서버에서 조회하여 설정
                makeDocNo: null, // 서버에서 조회하여 설정
                requestId: userId, // REQUEST_ID는 userId
                requestEmpNo: empNo, // REQUEST_EMP_NO는 empNo
                eDocCode: eDocCode, // Oracle에서 조회한 값 사용
                eDocName: eDocName, // Oracle에서 조회한 값 사용
                requestName: requestName,
                existing_cust_code: existingCustCode, // 기존 거래처 코드 추가
                custName: custName, // 거래처명
                bizNo: bizNo, // 사업자등록번호
                bossName: bossName, // 대표자명
                tradeCls: tradeCls, // 거래구분
                bizCond: bizCond, // 업종
                bizKnd: bizKnd, // 업태
                zipCode: zipCode, // 우편번호
                addr1: addr1, // 주소1
                addr2: addr2, // 주소2
                telNo: telNo, // 전화번호
                headFax: headFax, // 팩스번호
                bankMainCode: bankMainCode, // 은행코드
                bankCode: bankCode, // 은행코드
                custAccNo: custAccNo, // 계좌번호
                elctTag: elctTag, // 전자세금계산서 여부
                cOwner: cOwner, // 소유자
                remark: remark, // 비고
                taxCls: taxCls, // 세금구분
                representCustCode: representCustCode, // 대표거래처코드
                sBankNo: sBankNo, // 은행번호
                regCls: regCls, // 등록구분
                exportTag: 'H',
                reportFileName: `draft_${mstSeq}.html`,
                parameters: JSON.stringify({
                    eaId: eaId,
                    deptId: deptId,
                    projCode: projCode,
                    projName: projName,
                    requestName: requestName,
                    requestDate: new Date().toISOString().split('T')[0]
                }),
                eDocUrl: null,  // 서버에서 조회하여 설정
                eDocFileName: null,  // 서버에서 조회하여 설정
                eConfStatus: '00',
                remarks: '',
                gjMsbh: '',
                refNm1: '',
                refNm2: '',
                refNm3: '',
                refNm4: '',
                refNm5: '',
                refUrl1: '',
                refUrl2: '',
                refUrl3: '',
                refUrl4: '',
                refUrl5: '',
                makeProjName: projName,
                requestIdName: userName,
                eaId: eaId,
                gjSeq: ''
            };

            // 디버깅: 전송할 erpData 확인
            console.log('=== 전송할 erpData 확인 ===');
            console.log('erpData.custName:', erpData.custName);
            console.log('erpData.bizNo:', erpData.bizNo);
            console.log('erpData.existing_cust_code:', erpData.existing_cust_code);
            console.log('erpData 전체:', erpData);
            console.log('============================');
            
            // 서버 API를 통해 ERP 프로시저 호출
            $.ajax({
                url: `${this.config.API_BASE_URL}/send-to-erp`,
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(erpData),
                success: function(response) {
                    console.log('✅ ERP 프로시저 호출 성공:', response);
                    if (response.success) {
                        console.log('🎉 ERP 전송 완료! 메시지:', response.message);
                        
                        // ERP 전송 성공 후 성공 콜백 호출
                        if (onSuccess) {
                            onSuccess(response.message || 'ERP 전송 완료');
                        }
                    } else {
                        console.log('⚠️ ERP 전송 실패! 메시지:', response.message);
                        
                        // ERP 전송 실패 시 에러 콜백 호출
                        if (onError) {
                            onError(response.message || 'ERP 전송 실패');
                        }
                    }
                },
                error: function(xhr, status, error) {
                    console.error('❌ ERP 프로시저 호출 실패:', error);
                    console.error('상태:', status);
                    console.error('응답:', xhr.responseText);
                    
                    // ERP 전송 에러 시 에러 콜백 호출
                    if (onError) {
                        onError('ERP 프로시저 호출 실패: ' + error);
                    }
                }
            });
        }

        /**
         * 에러 처리
         * @param {string} message - 에러 메시지
         * @param {Function} onError - 에러 콜백
         */
        handleError(message, onError) {
            console.error('ApprovalDocumentGenerator Error:', message);
            if (onError) {
                onError(message);
            } else {
                alert(`오류가 발생했습니다: ${message}`);
            }
        }
    }

    // jQuery 플러그인으로 등록
    $.approvalDocument = {
        generator: ApprovalDocumentGenerator
    };

    // 전역 함수로도 노출
    window.ApprovalDocumentGenerator = ApprovalDocumentGenerator;

})(jQuery); 