package com.dage.rent.config;

import com.dage.rent.Service.ContractFileMigrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * contract_d 파일 참조 → attachment_file 마이그레이션을 애플리케이션 기동 시 한 번 실행할 수 있게 합니다.
 * application.properties 에 migration.run-contract-file-migration=true 로 설정 후 기동하면 실행됩니다.
 */
@Configuration
public class ContractFileMigrationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContractFileMigrationRunner.class);

    @Bean
    public CommandLineRunner contractFileMigrationRunnerBean(
            ContractFileMigrationService migrationService,
            @Value("${migration.run-contract-file-migration:false}") boolean runMigration) {
        return args -> {
            if (!runMigration) {
                return;
            }
            log.info("[마이그레이션] contract_d → attachment_file 마이그레이션 시작");
            try {
                int[] result = migrationService.runMigration();
                log.info("[마이그레이션] 완료 - contract_d 처리: {}, attachment_file 신규 등록: {}, 건너뛴 파일: {}",
                        result[0], result[1], result[2]);
            } catch (Exception e) {
                log.error("[마이그레이션] 실패", e);
                throw e;
            }
        };
    }
}
