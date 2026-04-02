package com.dage.rent.Service;

import com.dage.rent.Component.FileEncryptionUtil;
import com.dage.rent.DAO.mysql.AttachmentFileDAO;
import com.dage.rent.DTO.AttachmentFileDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentFileService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentFileService.class);

    private final AttachmentFileDAO attachmentFileDAO;
    private final String uploadDir;
    private final String encryptionKeyBase64;
    private FileEncryptionUtil encryptionUtil;
    private static final String ENCRYPTED_SUBDIR = "encrypted";
    /** 디스크 저장용 폴더명 (한글 폴더명 방지) */
    private static final String FOLDER_REAL_ESTATE = "real_estate";
    private static final String FOLDER_CREDIT = "credit";

    /**
     * 섹션(영문/한글) → 실제 저장 폴더명(영문). DB·API는 영문(real_estate, credit) 사용.
     */
    private static String sectionToStorageFolder(String section) {
        if (FOLDER_REAL_ESTATE.equals(section) || "부동산정보".equals(section)) return FOLDER_REAL_ESTATE;
        if (FOLDER_CREDIT.equals(section) || "채권순위".equals(section)) return FOLDER_CREDIT;
        return section != null ? section.replaceAll("[^a-zA-Z0-9_-]", "_") : "etc";
    }

    public AttachmentFileService(
            AttachmentFileDAO attachmentFileDAO,
            @Value("${file.upload-dir}") String uploadDir,
            @Value("${file.encryption-key:}") String encryptionKeyBase64) {
        this.attachmentFileDAO = attachmentFileDAO;
        this.uploadDir = uploadDir;
        this.encryptionKeyBase64 = encryptionKeyBase64;
    }

    @PostConstruct
    public void init() {
        if (encryptionKeyBase64 != null && !encryptionKeyBase64.isBlank()) {
            this.encryptionUtil = new FileEncryptionUtil(encryptionKeyBase64);
        }
        // 첫 업로드 요청 시 디렉터리 생성 실패/지연 방지: 기동 시 미리 생성
        try {
            Path base = Paths.get(uploadDir);
            Files.createDirectories(base.resolve(ENCRYPTED_SUBDIR).resolve(FOLDER_REAL_ESTATE));
            Files.createDirectories(base.resolve(ENCRYPTED_SUBDIR).resolve(FOLDER_CREDIT));
        } catch (Exception e) {
            log.warn("업로드 디렉터리 사전 생성 실패 (첫 업로드 시 생성 시도): {}", e.getMessage());
        }
    }

    /**
     * 암호화 가능 여부 (키가 설정된 경우에만 암호화)
     */
    public boolean isEncryptionEnabled() {
        return encryptionUtil != null;
    }

    /**
     * 업로드: 디스크에는 암호화된 파일로 저장, DB에는 원본명·저장경로 등 저장 후 id 반환
     */
    @Transactional(value = "mysqlTransactionManager", rollbackFor = Exception.class)
    public AttachmentFileDTO saveFile(MultipartFile file, String section) throws Exception {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        long fileSize = file.getSize();
        String contentType = file.getContentType();

        String storedFilename = UUID.randomUUID().toString().replace("-", "");
        String storageFolder = sectionToStorageFolder(section);
        String relativePath = ENCRYPTED_SUBDIR + "/" + storageFolder + "/" + storedFilename;
        Path fullPath = Paths.get(uploadDir).resolve(relativePath);
        Path parent = fullPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        byte[] bytes = file.getBytes();
        if (isEncryptionEnabled()) {
            bytes = encryptionUtil.encrypt(bytes);
        }
        Files.write(fullPath, bytes);

        AttachmentFileDTO dto = new AttachmentFileDTO();
        dto.setOriginalFilename(originalFilename);
        dto.setStoredFilename(storedFilename);
        dto.setStoredPath(relativePath);
        dto.setSection(sectionToStorageFolder(section)); // DB에는 항상 영문(real_estate, credit)
        dto.setFileSize(fileSize);
        dto.setContentType(contentType);
        dto.setEncrypted(isEncryptionEnabled()); // 저장 시 암호화했으면 플래그 설정 → 불러올 때만 복호화
        dto.setContractSeq(null);
        attachmentFileDAO.insert(dto);
        return dto;
    }

    /**
     * id로 메타정보 조회
     */
    public AttachmentFileDTO findById(Long id) {
        return attachmentFileDAO.findById(id);
    }

    /**
     * id 목록으로 메타정보 목록 조회 (원본 파일명 등 표시용)
     */
    public List<AttachmentFileDTO> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return attachmentFileDAO.findByIds(ids);
    }

    /**
     * id로 파일 바이트 로드 (복호화 후 반환)
     */
    public byte[] loadFileBytes(Long id) throws Exception {
        AttachmentFileDTO dto = attachmentFileDAO.findById(id);
        if (dto == null) return null;
        Path fullPath = Paths.get(uploadDir).resolve(dto.getStoredPath());
        if (!Files.exists(fullPath)) return null;
        byte[] bytes = Files.readAllBytes(fullPath);
        // DB에 암호화 여부가 저장되어 있으므로, 암호화된 파일만 복호화 (기존 평문 파일과 구분)
        if (Boolean.TRUE.equals(dto.getEncrypted()) && encryptionUtil != null) {
            bytes = encryptionUtil.decrypt(bytes);
        }
        return bytes;
    }

    /**
     * id로 파일 삭제 (DB + 디스크)
     */
    @Transactional(value = "mysqlTransactionManager", rollbackFor = Exception.class)
    public boolean deleteById(Long id) throws IOException {
        AttachmentFileDTO dto = attachmentFileDAO.findById(id);
        if (dto == null) return false;
        Path fullPath = Paths.get(uploadDir).resolve(dto.getStoredPath());
        if (Files.exists(fullPath)) {
            Files.delete(fullPath);
        }
        attachmentFileDAO.deleteById(id);
        return true;
    }

    /**
     * 저장 시 contract_seq 연결
     */
    @Transactional(value = "mysqlTransactionManager", rollbackFor = Exception.class)
    public void linkToContract(List<Long> fileIds, Integer contractSeq) {
        if (fileIds == null || fileIds.isEmpty() || contractSeq == null) return;
        attachmentFileDAO.updateContractSeqByIds(fileIds, contractSeq);
    }

    /**
     * 마이그레이션: 디스크에 있는 레거시 파일을 attachment_file에 등록.
     * 한글 폴더(부동산정보, 채권순위)에 있으면 영문 폴더(real_estate, credit)로 이동 후 등록하여, 마이그레이션 후 한글 폴더 제거 가능.
     * @return 등록된 DTO(id 포함), 파일이 없으면 null
     */
    @Transactional(value = "mysqlTransactionManager", rollbackFor = Exception.class)
    public AttachmentFileDTO registerLegacyFile(String filename, String section, Integer contractSeq) throws IOException {
        if (filename == null || (filename = filename.trim()).isEmpty()) return null;
        // 경로가 포함된 경우 파일명만 사용 (예: 부동산정보/파일.pdf → 파일.pdf)
        String nameOnly = filename.replace('\\', '/');
        int lastSlash = nameOnly.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < nameOnly.length() - 1) {
            nameOnly = nameOnly.substring(lastSlash + 1).trim();
        }
        if (nameOnly.isEmpty()) return null;

        boolean isRealEstate = FOLDER_REAL_ESTATE.equals(section) || "부동산정보".equals(section);
        String[] toSearch = isRealEstate
                ? new String[]{FOLDER_REAL_ESTATE, "부동산정보"}
                : new String[]{FOLDER_CREDIT, "채권순위"};
        Path filePath = null;
        String usedFolder = null;
        Path uploadBase = Paths.get(uploadDir).toAbsolutePath().normalize();
        for (String folder : toSearch) {
            Path p = uploadBase.resolve(folder).resolve(nameOnly);
            if (Files.exists(p) && Files.isRegularFile(p)) {
                filePath = p;
                usedFolder = folder;
                break;
            }
        }
        if (filePath == null || usedFolder == null) {
            log.warn("[레거시 파일 미발견] section={}, filename={}, 시도 경로 기준 dir={}, 검색 폴더={}",
                    section, filename, uploadBase, String.join(", ", toSearch));
            return null;
        }
        // 한글 폴더에 있으면 영문 폴더로 이동 → 마이그레이션 후 한글 폴더 제거 가능
        if ("부동산정보".equals(usedFolder) || "채권순위".equals(usedFolder)) {
            String targetFolder = "부동산정보".equals(usedFolder) ? FOLDER_REAL_ESTATE : FOLDER_CREDIT;
            Path targetDir = uploadBase.resolve(targetFolder);
            if (Files.notExists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            Path targetPath = targetDir.resolve(nameOnly);
            Files.move(filePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            filePath = targetPath;
            usedFolder = targetFolder;
        }
        long fileSize = Files.size(filePath);
        String relativePath = usedFolder + "/" + nameOnly;
        AttachmentFileDTO dto = new AttachmentFileDTO();
        dto.setOriginalFilename(filename);
        dto.setStoredFilename(nameOnly);
        dto.setStoredPath(relativePath);
        dto.setSection(sectionToStorageFolder(section)); // DB에는 항상 영문
        dto.setFileSize(fileSize);
        dto.setContentType(null);
        dto.setEncrypted(false);
        dto.setContractSeq(contractSeq);
        attachmentFileDAO.insert(dto);
        return dto;
    }

    /**
     * real_estate_files / credit_files 문자열에서 숫자 id 목록 추출 (기존 "원본명;원본명" 형식은 빈 리스트)
     */
    public static List<Long> parseFileIds(String semicolonSeparated) {
        List<Long> ids = new ArrayList<>();
        if (semicolonSeparated == null || semicolonSeparated.isBlank()) return ids;
        for (String s : semicolonSeparated.split(";")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            try {
                ids.add(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
                // 레거시 원본파일명이면 무시
            }
        }
        return ids;
    }
}
