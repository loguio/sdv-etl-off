package com.example.etloff;

import com.example.etloff.service.EtlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EtlRunner implements CommandLineRunner {

    private final EtlService etlService;

    @Override
    public void run(String... args) throws Exception {
        log.info("EtlRunner: starting ingestion on application startup...");
        try {
            etlService.importData("open-food-facts.csv");
        } catch (Exception e) {
            log.error("ETL Ingestion failed: ", e);
        }
    }
}
