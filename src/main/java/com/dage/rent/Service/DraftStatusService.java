package com.dage.rent.Service;

import com.dage.rent.DAO.mysql.DraftDAO;
import com.dage.rent.DTO.DraftDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DraftStatusService {
    
    @Autowired
    private DraftDAO draftDAO;
    
    @Autowired
    private RentService rentService;
    
    public enum DraftStatus {
        DRAFT("1", "기안작성"),
        UPLOADING("2", "기안업로드"),
        COMPLETED("3", "기안완료");
        
        private final String code;
        private final String description;
        
        DraftStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
        
        // 코드로부터 상태 찾기
        public static DraftStatus fromCode(String code) {
            for (DraftStatus status : values()) {
                if (status.code.equals(code)) {
                    return status;
                }
            }
            return DRAFT; // 기본값
        }
        
        // 한글 표시용
        public String getDisplayName() {
            switch (this) {
                case DRAFT: return "작성";
                case UPLOADING: return "업로드";
                case COMPLETED: return "완료";
                default: return "작성";
            }
        }
    }
    
    // status 컬럼이 제거되어 더 이상 사용하지 않음
    // 상태 관리는 mst_seq 유무와 Oracle의 conf_status로 판단
    
    /**
     * 기안 미작성 목록 조회 (mst_seq가 없는 경우 - 아직 업로드 안 함)
     */
    public List<DraftDTO> getDraftPendingList(String userNo) {
        return draftDAO.getDraftPendingList(userNo);
    }
    
    /**
     * 기안 미완료 목록 조회 (업로드 필요: conf_status = 00, 40, 99)
     */
    public List<DraftDTO> getDraftIncompleteList(String userNo) {
        List<DraftDTO> allDrafts = draftDAO.getDraftIncompleteList(userNo);
        List<DraftDTO> filteredList = new ArrayList<>();
        
        for (DraftDTO draft : allDrafts) {
            if (draft.getMst_seq() != null && !draft.getMst_seq().trim().isEmpty()) {
                try {
                    String confStatus = rentService.getConfStatus(draft.getMst_seq());
                    // 공백 제거 (Oracle에서 반환되는 값에 공백이 포함될 수 있음)
                    if (confStatus != null) {
                        confStatus = confStatus.trim();
                    }
                    // 00(미전송), 40(반려), 99(삭제) 인 경우만 포함
                    if ("00".equals(confStatus) || "40".equals(confStatus) || "99".equals(confStatus)) {
                        filteredList.add(draft);
                    }
                } catch (Exception e) {
                    System.err.println("conf_status 조회 실패 - mst_seq: " + draft.getMst_seq());
                    // 조회 실패 시 포함 (미전송으로 간주)
                    filteredList.add(draft);
                }
            }
        }
        
        return filteredList;
    }
    
    /**
     * 기안완료 목록 조회 (결재 진행/완료: conf_status = 10, 20, 30)
     */
    public List<DraftDTO> getDraftCompletedList(String userNo) {
        List<DraftDTO> allDrafts = draftDAO.getDraftCompletedList(userNo);
        List<DraftDTO> filteredList = new ArrayList<>();
        
        for (DraftDTO draft : allDrafts) {
            if (draft.getMst_seq() != null && !draft.getMst_seq().trim().isEmpty()) {
                try {
                    String confStatus = rentService.getConfStatus(draft.getMst_seq());
                    // 공백 제거 (Oracle에서 반환되는 값에 공백이 포함될 수 있음)
                    if (confStatus != null) {
                        confStatus = confStatus.trim();
                    }
                    // 10(결재중), 20(보류), 30(완료) 인 경우만 포함
                    if ("10".equals(confStatus) || "20".equals(confStatus) || "30".equals(confStatus)) {
                        filteredList.add(draft);
                    }
                } catch (Exception e) {
                    System.err.println("conf_status 조회 실패 - mst_seq: " + draft.getMst_seq());
                    // 조회 실패 시 제외
                }
            }
        }
        
        return filteredList;
    }
}
