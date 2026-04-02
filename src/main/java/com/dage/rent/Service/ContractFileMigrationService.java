package com.dage.rent.Service;

import com.dage.rent.DAO.mysql.ContractDAO;
import com.dage.rent.DTO.AttachmentFileDTO;
import com.dage.rent.DTO.ContractDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * contract_d의 real_estate_files, credit_files(원본 파일명 문자열)를
 * attachment_file 행으로 등록하고, contract_d에는 새 id 목록으로 갱신하는 마이그레이션.
 */
@Service
public class ContractFileMigrationService {

    private final ContractDAO contractDAO;
    private final AttachmentFileService attachmentFileService;

    public ContractFileMigrationService(ContractDAO contractDAO, AttachmentFileService attachmentFileService) {
        this.contractDAO = contractDAO;
        this.attachmentFileService = attachmentFileService;
    }

    /**
     * 마이그레이션 실행.
     * - 이미 숫자 id만 있는 행(이미 이전 마이그레이션 적용)은 건너뜀.
     * - 파일이 디스크에 없으면 해당 항목만 건너뛰고 계속 진행.
     * @return [처리한 contract_d 건수, 새로 등록한 attachment_file 건수, 건너뛴 파일 수]
     */
    @Transactional(value = "mysqlTransactionManager", rollbackFor = Exception.class)
    public int[] runMigration() throws IOException {
        List<ContractDTO> list = contractDAO.selectContractDWithFileReferences();
        int contractsProcessed = 0;
        int filesRegistered = 0;
        int filesSkipped = 0;

        for (ContractDTO row : list) {
            int seq = Integer.parseInt(row.getSeq());
            String realStr = row.getReal_estate_files() != null ? row.getReal_estate_files().trim() : "";
            String creditStr = row.getCredit_files() != null ? row.getCredit_files().trim() : "";

            // 이미 숫자 id만 있으면 마이그레이션 완료된 행으로 간주
            if (isAlreadyMigrated(realStr) && isAlreadyMigrated(creditStr)) {
                continue;
            }

            List<String> newRealIds = new ArrayList<>();
            List<String> newCreditIds = new ArrayList<>();
            boolean rowHadSkip = false;

            // DB에 "파일1;파일2" 또는 "파일1,파일2" 형태로 저장된 경우 모두 개별 파일로 처리
            for (String filename : splitFileReferences(realStr)) {
                if (isNumeric(filename)) {
                    newRealIds.add(filename);
                    continue;
                }
                AttachmentFileDTO dto = attachmentFileService.registerLegacyFile(filename, "부동산정보", seq);
                if (dto != null && dto.getId() != null) {
                    newRealIds.add(String.valueOf(dto.getId()));
                    filesRegistered++;
                } else {
                    filesSkipped++;
                    rowHadSkip = true;
                }
            }
            for (String filename : splitFileReferences(creditStr)) {
                if (isNumeric(filename)) {
                    newCreditIds.add(filename);
                    continue;
                }
                AttachmentFileDTO dto = attachmentFileService.registerLegacyFile(filename, "채권순위", seq);
                if (dto != null && dto.getId() != null) {
                    newCreditIds.add(String.valueOf(dto.getId()));
                    filesRegistered++;
                } else {
                    filesSkipped++;
                    rowHadSkip = true;
                }
            }

            // 파일을 찾지 못한 항목이 있으면 해당 행은 갱신하지 않음 → 기존 파일명 참조 유지
            if (rowHadSkip) {
                continue;
            }
            String newReal = String.join(";", newRealIds);
            String newCredit = String.join(";", newCreditIds);
            contractDAO.updateContractFileReferences(seq, newReal, newCredit);
            contractsProcessed++;
        }

        return new int[]{contractsProcessed, filesRegistered, filesSkipped};
    }

    /** 세미콜론(;)만 구분자로 사용. 파일명에 쉼표가 들어가는 경우가 많아 쉼표는 구분자로 쓰지 않음 */
    private static List<String> splitFileReferences(String semicolonSeparated) {
        List<String> list = new ArrayList<>();
        if (semicolonSeparated == null || semicolonSeparated.isEmpty()) return list;
        for (String part : semicolonSeparated.split(";")) {
            String s = part.trim();
            if (!s.isEmpty()) list.add(s);
        }
        return list;
    }

    private static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    /** 이미 id만 있는지 (모든 토큰이 숫자) */
    private static boolean isAlreadyMigrated(String semicolonSeparated) {
        if (semicolonSeparated == null || semicolonSeparated.trim().isEmpty()) return true;
        for (String s : semicolonSeparated.split(";")) {
            if (!s.trim().isEmpty() && !isNumeric(s.trim())) return false;
        }
        return true;
    }
}
