package com.dage.rent.DAO.mysql;

import com.dage.rent.DTO.DraftDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DraftDAO {

    // 기안서 저장
    int saveDraft(DraftDTO draftDTO);
    
    // draftId로 custCode 조회
    String getCustCodeByDraftId(@Param("draftId") String draftId);



    // 계약내용 상세 저장
    int saveContractDetail(DraftDTO.ContractDetailDTO contractDetail);

    // 기안서 조회 (ID로)
    DraftDTO getDraftById(@Param("id") int id);



    // 기안서 조회 (승인번호로)
    DraftDTO getDraftByApprNo(@Param("appr_no") int apprNo);

    // 기안서 조회 (mstSeq로)
    DraftDTO getDraftByMstSeq(@Param("mst_seq") String mstSeq);

    // 계약내용 상세 조회
    List<DraftDTO.ContractDetailDTO> getContractDetailsByDraftId(@Param("draftId") int draftId);

    // 기안서 목록 조회
    List<DraftDTO> getDraftList(Map<String, Object> params);

    // 기안서 수정
    int updateDraft(DraftDTO draftDTO);



    // 계약내용 상세 삭제
    int deleteContractDetailsByDraftId(@Param("draftId") int draftId);

    // 기안서 삭제
    int deleteDraft(@Param("id") int id);
    
    // 드래프트 ID로 드래프트 조회 (Long 타입 지원)
    DraftDTO getDraftByIdLong(@Param("id") Long id);
    
    // 기안 미작성 목록 조회 (mst_seq가 없는 경우)
    List<DraftDTO> getDraftPendingList(@Param("user_no") String userNo);
    
    // 기안 미완료 목록 조회 (업로드 필요: conf_status = 00, 40, 99)
    List<DraftDTO> getDraftIncompleteList(@Param("user_no") String userNo);
    
    // 기안완료 목록 조회 (결재 진행/완료: conf_status = 10, 20, 30)
    List<DraftDTO> getDraftCompletedList(@Param("user_no") String userNo);
    
    // ERP 등록 상태 업데이트 (기존 호환성 유지)
    int updateErpRegStatus(@Param("draftId") Long draftId, @Param("erpReg") String erpReg);
    
    // mst_seq 업데이트 (업로드 시점에만 생성)
    int updateDraftMstSeq(Map<String, Object> params);

} 