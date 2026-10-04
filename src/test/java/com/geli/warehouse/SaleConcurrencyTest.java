package com.geli.warehouse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SaleConcurrencyTest {
    private static final int ATTEMPTS = 20;
    private static final int STOCK = 5;

    private static final String DATABASE_URL =
            "jdbc:sqlite:./target/test-concurrency-" + UUID.randomUUID()
                    + ".db?foreign_keys=on&busy_timeout=10000&journal_mode=WAL&transaction_mode=IMMEDIATE";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DATABASE_URL);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void neverOversellsUnderConcurrentRequests() throws Exception {
        int variantId = createVariantWithStock(STOCK);

        ExecutorService pool = Executors.newFixedThreadPool(ATTEMPTS);
        CountDownLatch startTogether = new CountDownLatch(1);
        List<Callable<Integer>> attempts = new ArrayList<>();
        for (int i = 0; i < ATTEMPTS; i++) {
            attempts.add(() -> {
                startTogether.await();
                return postJson("/api/v1/sales", """
                        {"lines":[{"variantId":%d,"quantity":1}]}
                        """.formatted(variantId)).getStatusCode().value();
            });
        }

        List<Future<Integer>> results = new ArrayList<>();
        for (Callable<Integer> attempt : attempts) {
            results.add(pool.submit(attempt));
        }
        startTogether.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        int created = 0;
        int rejected = 0;
        for (Future<Integer> result : results) {
            int status = result.get();
            if (status == HttpStatus.CREATED.value()) {
                created++;
            } else if (status == HttpStatus.CONFLICT.value()) {
                rejected++;
            }
        }

        assertThat(created).isEqualTo(STOCK);
        assertThat(rejected).isEqualTo(ATTEMPTS - STOCK);
        assertThat(stockOf(variantId)).isZero();
        assertThat(saleMovementsOf(variantId)).isEqualTo(STOCK);
    }

    private ResponseEntity<String> postJson(String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(url(path), new HttpEntity<>(body, headers), String.class);
    }

    private int createVariantWithStock(int stock) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        String itemBody = postJson("/api/v1/items", """
                {"sku":"CONC-ITEM-%s","name":"Concurrency item","basePrice":10000}
                """.formatted(suffix)).getBody();
        int itemId = objectMapper.readTree(itemBody).get("id").asInt();

        String variantBody = postJson("/api/v1/items/" + itemId + "/variants", """
                {"sku":"CONC-VAR-%s","name":"Only variant","initialStock":%d}
                """.formatted(suffix, stock)).getBody();
        return objectMapper.readTree(variantBody).get("id").asInt();
    }

    private int stockOf(int variantId) throws Exception {
        String body = restTemplate.getForEntity(url("/api/v1/variants/" + variantId), String.class).getBody();
        return objectMapper.readTree(body).get("stock").asInt();
    }

    private long saleMovementsOf(int variantId) throws Exception {
        String body = restTemplate.getForEntity(
                url("/api/v1/variants/" + variantId + "/stock-movements?size=100"), String.class).getBody();
        long count = 0;
        for (JsonNode movement : objectMapper.readTree(body).get("content")) {
            if ("SALE".equals(movement.get("type").asText())) {
                count++;
            }
        }
        return count;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
