package com.dage.rent.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttachmentFileDTO {
    private Long id;
    private String originalFilename;
    private String storedFilename;
    private String storedPath;
    private String section;
    private Long fileSize;
    private String contentType;
    /** true면 저장 시 암호화됨 → 불러올 때만 복호화 */
    private Boolean encrypted;
    private Integer contractSeq;
    private LocalDateTime createdAt;
}
