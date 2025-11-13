# 결재문서 생성 API 사용법

## 개요
사내 인트라넷 결재문서 생성을 위한 API와 JavaScript 라이브러리를 제공합니다.

## API 엔드포인트

### 1. 결재문서 HTML 생성 API
```
GET /api/approval/generate-document?draftId={기안서ID}
```

**응답 예시:**
```json
{
  "success": true,
  "documentHTML": "<!DOCTYPE html>...",
  "draftData": { ... },
  "eaId": "0123",
  "mstSeq": "12345",
  "requestName": "프로젝트명",
  "deptId": "0123"
}
```

### 2. 결재문서 데이터 조회 API
```
GET /api/approval/document-data?draftId={기안서ID}
```

## JavaScript 라이브러리 사용법

### 1. 라이브러리 로드
```html
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<script src="/js/approval-integration.js"></script>
```

### 2. 기본 사용법

#### 방법 1: 클래스 기반 사용
```javascript
// 생성자로 인스턴스 생성
const generator = new ApprovalDocumentGenerator({
    API_BASE_URL: '/api/approval',
    ERP_URL: 'http://derp.dage.co.kr/DAGE/unicon_ccms/ERPhtml01.jsp'
});

// 결재문서 생성 및 전송
generator.generateAndSend({
    draftId: 123,
    eaId: '0123',
    requestName: '프로젝트명',
    userId: 'user123',
    deptId: '0123',
    mstSeq: '12345',
    subSeq: '',
    eDocName: '임대차계약서',
    onSuccess: function(response) {
        console.log('결재문서 생성 완료:', response);
    },
    onError: function(error) {
        console.error('결재문서 생성 실패:', error);
    }
});
```

#### 방법 2: 함수 기반 사용 (레거시 호환성)
```javascript
generateApprovalDocument({
    txtEAID: document.getElementById('txtEAID'),
    txtREQUEST_NAME: document.getElementById('txtREQUEST_NAME'),
    txtREQUEST_ID: document.getElementById('txtREQUEST_ID'),
    txtGW_PROJ_CODE: document.getElementById('txtGW_PROJ_CODE'),
    frmList: document.getElementById('frmList'),
    txtE_DOC_NAME: document.getElementById('txtE_DOC_NAME'),
    onSuccess: function(response) {
        console.log('결재문서 생성 완료:', response);
    },
    onError: function(error) {
        console.error('결재문서 생성 실패:', error);
    }
});
```

#### 방법 3: jQuery 플러그인 사용
```javascript
$.approvalDocument.generate({
    txtEAID: $('#txtEAID')[0],
    txtREQUEST_NAME: $('#txtREQUEST_NAME')[0],
    txtREQUEST_ID: $('#txtREQUEST_ID')[0],
    txtGW_PROJ_CODE: $('#txtGW_PROJ_CODE')[0],
    frmList: $('#frmList')[0],
    txtE_DOC_NAME: $('#txtE_DOC_NAME')[0]
});
```

### 3. 기존 코드와의 호환성

기존 코드를 최소한으로 수정하여 사용할 수 있습니다:

```javascript
$(function(){
    // 기존 코드를 아래와 같이 변경
    const generator = new ApprovalDocumentGenerator();
    
    generator.generateAndSend({
        draftId: frmList_11.MST_SEQ.value,
        eaId: txtEAID.value,
        requestName: txtREQUEST_NAME.value,
        userId: txtREQUEST_ID.value,
        deptId: txtGW_PROJ_CODE.value,
        mstSeq: frmList_11.MST_SEQ.value,
        subSeq: frmList_11.SUB_SEQ.value,
        eDocName: txtE_DOC_NAME.value,
        onSuccess: function(response) {
            console.log('결재문서 생성 완료');
        },
        onError: function(error) {
            alert("error 발생!! " + error);
        }
    });
});
```

## 파라미터 설명

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| draftId | number | ✓ | 기안서 ID |
| eaId | string | ✓ | EA ID (그룹웨어 프로젝트 코드) |
| requestName | string | ✓ | 요청명 (프로젝트명) |
| userId | string | ✓ | 사용자 ID |
| deptId | string | ✓ | 부서 ID (그룹웨어 부서코드) |
| mstSeq | string | ✓ | 마스터 시퀀스 |
| subSeq | string | | 서브 시퀀스 (선택사항) |
| eDocName | string | | 전자문서명 (선택사항) |

## 에러 처리

라이브러리는 다음과 같은 에러 상황을 처리합니다:

1. **필수 파라미터 누락**: 기안서 ID, EA ID 등 필수 파라미터가 없는 경우
2. **API 호출 실패**: 결재문서 HTML 생성 API 호출 실패
3. **ERP 전송 실패**: ERP 시스템으로 전송 실패
4. **네트워크 오류**: 네트워크 연결 문제

## 설정 옵션

```javascript
const config = {
    API_BASE_URL: '/api/approval',           // API 기본 URL
    ERP_URL: 'http://derp.dage.co.kr/DAGE/unicon_ccms/ERPhtml01.jsp', // ERP URL
    DEFAULT_FORM_ID: 'frmList_11'            // 기본 폼 ID
};
```

## 브라우저 호환성

- jQuery 3.0+
- ES6+ 지원 브라우저 (Chrome 60+, Firefox 55+, Safari 12+, Edge 79+)

## 주의사항

1. **CORS 설정**: 다른 도메인에서 API를 호출할 경우 CORS 설정이 필요합니다.
2. **인증**: API 호출 시 적절한 인증이 필요할 수 있습니다.
3. **ERP URL**: ERP 시스템 URL이 올바른지 확인하세요.
4. **기안서 ID**: 유효한 기안서 ID를 사용해야 합니다.

## 예시 프로젝트

다른 프로젝트에서 사용하는 예시:

```html
<!DOCTYPE html>
<html>
<head>
    <title>결재문서 생성 예시</title>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="http://your-domain.com/js/approval-integration.js"></script>
</head>
<body>
    <button onclick="generateDocument()">결재문서 생성</button>
    
    <script>
        function generateDocument() {
            const generator = new ApprovalDocumentGenerator();
            
            generator.generateAndSend({
                draftId: 123,
                eaId: '0123',
                requestName: '테스트 프로젝트',
                userId: 'testuser',
                deptId: '0123',
                mstSeq: '12345',
                onSuccess: function(response) {
                    alert('결재문서가 성공적으로 생성되었습니다.');
                },
                onError: function(error) {
                    alert('오류가 발생했습니다: ' + error);
                }
            });
        }
    </script>
</body>
</html>
``` 