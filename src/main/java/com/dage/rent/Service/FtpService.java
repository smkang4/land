package com.dage.rent.Service;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FtpService {
    
    @Value("${ftp.host}")
    private String ftpHost;
    
    @Value("${ftp.port}")
    private int ftpPort;
    
    @Value("${ftp.username}")
    private String ftpUsername;
    
    @Value("${ftp.password}")
    private String ftpPassword;
    
    @Value("${ftp.remote-directory}")
    private String remoteDirectory;
    
    @Value("${windows.share.path}")
    private String windowsSharePath;
    
    /**
     * FTP 서버의 디렉토리 구조 확인
     * @return 디렉토리 목록
     */
    public String listDirectories() {
        FTPClient ftpClient = new FTPClient();
        try {
            // FTP 서버 연결
            ftpClient.connect(ftpHost, ftpPort);
            
            // 연결 상태 확인
            if (!ftpClient.isConnected()) {
                System.err.println("FTP 서버 연결 실패");
                return "에러: FTP 서버 연결 실패";
            }
            
            // 로그인 시도
            boolean loginSuccess = ftpClient.login(ftpUsername, ftpPassword);
            int replyCode = ftpClient.getReplyCode();
            System.out.println("FTP 서버 응답 코드: " + replyCode);
            System.out.println("로그인 성공 여부: " + loginSuccess);
            
            // 로그인 실패 시 종료
            if (!loginSuccess) {
                System.err.println("FTP 로그인 실패 - 응답 코드: " + replyCode);
                System.err.println("사용자명: " + ftpUsername);
                System.err.println("비밀번호 길이: " + (ftpPassword != null ? ftpPassword.length() : "null"));
                System.err.println("비밀번호 첫글자: " + (ftpPassword != null && ftpPassword.length() > 0 ? ftpPassword.charAt(0) : "none"));
                System.err.println("비밀번호 마지막글자: " + (ftpPassword != null && ftpPassword.length() > 0 ? ftpPassword.charAt(ftpPassword.length()-1) : "none"));
                return "에러: FTP 로그인 실패 - 응답 코드: " + replyCode;
            }
            
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            
            // 연결 후 잠시 대기
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("-- FTP 서버 연결 성공 --");
            System.out.println("FTP 서버: " + ftpHost + ":" + ftpPort);
            System.out.println("사용자: " + ftpUsername);
            System.out.println("현재 작업 디렉토리: " + ftpClient.printWorkingDirectory());
            System.out.println("FTP 서버 응답: " + ftpClient.getReplyString());
            System.out.println("로컬 IP 확인: " + ftpClient.getLocalAddress());
            System.out.println("원격 IP 확인: " + ftpClient.getRemoteAddress());
            
            // FTP 서버 시스템 정보 확인
            try {
                String systemInfo = ftpClient.getSystemType();
                System.out.println("FTP 서버 시스템: " + systemInfo);
            } catch (Exception e) {
                System.out.println("시스템 정보 조회 실패: " + e.getMessage());
            }
            
            // 루트 디렉토리 내용 조회
            String[] rootFiles = ftpClient.listNames();
            System.out.println("=== 루트 디렉토리 내용 ===");
            if (rootFiles != null) {
                for (String file : rootFiles) {
                    System.out.println("  " + file);
                }
            }
            
            // dage 디렉토리로 이동 시도 (소문자)
            if (ftpClient.changeWorkingDirectory("dage")) {
                System.out.println("dage 디렉토리 접근 성공");
                String[] dageFiles = ftpClient.listNames();
                System.out.println("=== dage 디렉토리 내용 ===");
                if (dageFiles != null) {
                    for (String file : dageFiles) {
                        System.out.println("  " + file);
                    }
                }
                
                // unicon_gw 디렉토리로 이동 시도
                if (ftpClient.changeWorkingDirectory("unicon_gw")) {
                    System.out.println("unicon_gw 디렉토리 접근 성공");
                    String[] uniconFiles = ftpClient.listNames();
                    System.out.println("=== unicon_gw 디렉토리 내용 ===");
                    if (uniconFiles != null) {
                        for (String file : uniconFiles) {
                            System.out.println("  " + file);
                        }
                    }
                    
                    // gw_doc_file 디렉토리로 이동 시도
                    if (ftpClient.changeWorkingDirectory("gw_doc_file")) {
                        System.out.println("gw_doc_file 디렉토리 접근 성공");
                        String[] gwFiles = ftpClient.listNames();
                        System.out.println("=== gw_doc_file 디렉토리 내용 ===");
                        if (gwFiles != null) {
                            for (String file : gwFiles) {
                                System.out.println("  " + file);
                            }
                        }
                    } else {
                        System.out.println("gw_doc_file 디렉토리 접근 실패");
                    }
                } else {
                    System.out.println("unicon_gw 디렉토리 접근 실패");
                }
            } else {
                System.out.println("dage 디렉토리 접근 실패");
            }
            
            return "디렉토리 조회 완료";
            
        } catch (Exception e) {
            System.err.println("FTP 디렉토리 조회 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return "에러: " + e.getMessage();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * HTML 파일을 Windows 네트워크 공유에 업로드
     * @param htmlContent HTML 내용
     * @param fileName 파일명 (예: 202508/20250801-PROJ-001.htm)
     * @return 업로드 성공 여부
     */
    public boolean uploadHtmlFile(String htmlContent, String fileName) {
        FTPClient ftpClient = new FTPClient();
        try {
            // FTP 서버 연결
            ftpClient.connect(ftpHost, ftpPort);
            
            // 연결 상태 확인
            if (!ftpClient.isConnected()) {
                System.err.println("FTP 서버 연결 실패");
                return false;
            }
            
            // 로그인 시도
            boolean loginSuccess = ftpClient.login(ftpUsername, ftpPassword);
            int replyCode = ftpClient.getReplyCode();
            System.out.println("FTP 서버 응답 코드: " + replyCode);
            System.out.println("로그인 성공 여부: " + loginSuccess);
            
            // 로그인 실패 시 종료
            if (!loginSuccess) {
                System.err.println("FTP 로그인 실패 - 응답 코드: " + replyCode);
                System.err.println("사용자명: " + ftpUsername);
                System.err.println("비밀번호 길이: " + (ftpPassword != null ? ftpPassword.length() : "null"));
                System.err.println("비밀번호 첫글자: " + (ftpPassword != null && ftpPassword.length() > 0 ? ftpPassword.charAt(0) : "none"));
                System.err.println("비밀번호 마지막글자: " + (ftpPassword != null && ftpPassword.length() > 0 ? ftpPassword.charAt(ftpPassword.length()-1) : "none"));
                return false;
            }
            
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            
            // 연결 후 잠시 대기
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("-- ERP 서버 연결 -- ");
            System.out.println("FTP 서버: " + ftpHost + ":" + ftpPort);
            System.out.println("사용자: " + ftpUsername);
            System.out.println("연결 상태: " + (ftpClient.isConnected() ? "연결됨" : "연결 안됨"));
            System.out.println("현재 작업 디렉토리: " + ftpClient.printWorkingDirectory());

            String yearMonth = fileName.substring(0, fileName.indexOf("/"));
            
            // 1. 기본 원격 디렉토리로 이동 (절대 경로 시도)
            if (!ftpClient.changeWorkingDirectory(remoteDirectory)) {
                System.out.println("절대 경로 접근 실패, 상대 경로로 시도: " + remoteDirectory);
                
                // 현재 루트 디렉토리 내용 확인
                String[] rootFiles = ftpClient.listNames();
                System.out.println("=== 현재 루트 디렉토리 내용 ===");
                if (rootFiles != null && rootFiles.length > 0) {
                    for (String file : rootFiles) {
                        System.out.println("  " + file);
                    }
                } else {
                    System.out.println("  (디렉토리 내용이 비어있음)");
                }
                
                // 현재 작업 디렉토리 확인
                String currentWorkingDir = ftpClient.printWorkingDirectory();
                System.out.println("현재 작업 디렉토리: " + currentWorkingDir);
                
                // FTP 서버가 특별한 설정일 수 있으므로 직접 경로 시도
                System.out.println("직접 경로 시도 시작...");
                
                // dage 디렉토리 직접 시도
                if (ftpClient.changeWorkingDirectory("dage")) {
                    System.out.println("dage 디렉토리 접근 성공");
                    String[] dageFiles = ftpClient.listNames();
                    System.out.println("=== dage 디렉토리 내용 ===");
                    if (dageFiles != null && dageFiles.length > 0) {
                        for (String file : dageFiles) {
                            System.out.println("  " + file);
                        }
                        
                        // unicon_gw 찾기
                        if (ftpClient.changeWorkingDirectory("unicon_gw")) {
                            System.out.println("unicon_gw 디렉토리 접근 성공");
                            String[] uniconFiles = ftpClient.listNames();
                            System.out.println("=== unicon_gw 디렉토리 내용 ===");
                            if (uniconFiles != null && uniconFiles.length > 0) {
                                for (String file : uniconFiles) {
                                    System.out.println("  " + file);
                                }
                                
                                if (ftpClient.changeWorkingDirectory("gw_doc_file")) {
                                    System.out.println("gw_doc_file 디렉토리 접근 성공");
                                    System.out.println("상대 경로로 디렉토리 이동 성공");
                                } else {
                                    System.err.println("gw_doc_file 디렉토리에 도달할 수 없습니다.");
                                    return false;
                                }
                            } else {
                                System.err.println("unicon_gw 디렉토리가 비어있습니다.");
                                return false;
                            }
                        } else {
                            System.err.println("unicon_gw 디렉토리에 도달할 수 없습니다.");
                            return false;
                        }
                    } else {
                        System.err.println("dage 디렉토리가 비어있습니다.");
                        return false;
                    }
                } else {
                    System.err.println("dage 디렉토리에 도달할 수 없습니다.");
                    return false;
                }
            } else {
                System.out.println("절대 경로로 디렉토리 이동 성공");
            }
            
            // 2. 년월 폴더로 이동
            if (!ftpClient.changeWorkingDirectory(yearMonth)) {
                System.err.println("저장 폴더를 찾을 수 없습니다: " + yearMonth);
                return false;
            }

            System.out.println("현재 작업 디렉토리: " + ftpClient.printWorkingDirectory());
            System.out.println("-- 폴더 이동 완료, 파일업로드 실행 -- ");

            // 파일 업로드
            try (InputStream inputStream = new ByteArrayInputStream(htmlContent.getBytes("UTF-8"))) {
                String actualFileName = fileName.substring(fileName.indexOf("/") + 1);
                boolean success = ftpClient.storeFile(actualFileName, inputStream);
                
                if (success) {
                    System.out.println("HTML 파일 업로드 성공: " + fileName);
                } else {
                    System.err.println("HTML 파일 업로드 실패: " + fileName);
                }
                
                return success;
            }
            
        } catch (Exception e) {
            System.err.println("FTP 업로드 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
                }
    }
    
    /**
     * HTML 파일을 Windows 네트워크 공유에 업로드
     * @param htmlContent HTML 내용
     * @param fileName 파일명 (예: 202508/20250801-PROJ-001.htm)
     * @return 업로드 성공 여부
     */
    public boolean uploadHtmlFileToWindowsShare(String htmlContent, String fileName) {
        try {
            System.out.println("-- Windows 네트워크 공유 연결 시도 --");
            System.out.println("공유 경로: " + windowsSharePath);
            
            // 년월 폴더 추출
            String yearMonth = fileName.substring(0, fileName.indexOf("/"));
            String actualFileName = fileName.substring(fileName.indexOf("/") + 1);
            
            // 전체 경로 구성
            String fullPath = windowsSharePath + "\\" + yearMonth + "\\" + actualFileName;
            System.out.println("전체 파일 경로: " + fullPath);
            
            // 파일 생성 및 쓰기
            Path filePath = Path.of(fullPath);
            
            // HTML 내용을 파일에 쓰기
            Files.write(filePath, htmlContent.getBytes("UTF-8"));
            
            System.out.println("HTML 파일 업로드 성공: " + fullPath);
            return true;
            
        } catch (Exception e) {
            System.err.println("Windows 네트워크 공유 업로드 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}
