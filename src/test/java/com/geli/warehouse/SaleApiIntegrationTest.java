package com.geli.warehouse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaleApiIntegrationTest {

    private static final String DATABASE_URL =
            "jdbc:sqlite:./target/test-sale-api-" + UUID.randomUUID()
                    + ".db?foreign_keys=on&busy_timeout=10000&journal_mode=WAL&transaction_mode=IMMEDIATE";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DATABASE_URL);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private record TestVariant(int itemId, int variantId) {
    }

    @Test
    void createsSaleDecrementsStockAndWritesLedger() throws Exception {
        int saleId = createSale("""
                {"lines":[{"variantId":1,"quantity":2},{"variantId":6,"quantity":1}]}
                """);

        mockMvc.perform(get("/api/v1/sales/" + saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(249000))
                .andExpect(jsonPath("$.lines.length()").value(2));

        assertThat(stockOf(1)).isEqualTo(23);
        assertThat(stockOf(6)).isEqualTo(14);

        JsonNode movement = firstMovementOf(1);
        assertThat(movement.get("type").asText()).isEqualTo("SALE");
        assertThat(movement.get("quantityDelta").asInt()).isEqualTo(-2);
        assertThat(movement.get("stockAfter").asInt()).isEqualTo(23);
        assertThat(movement.get("referenceId").asInt()).isEqualTo(saleId);

    }

    @Test
    void rejectsSaleWhenStockInsufficient() throws Exception {
        TestVariant variant = createTestVariant(1);

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"variantId":%d,"quantity":2}]}
                                """.formatted(variant.variantId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.error.details").isNotEmpty());

        assertThat(stockOf(variant.variantId())).isEqualTo(1);
        assertThat(saleMovementsOf(variant.variantId())).isZero();
    }

    @Test
    void rollsBackWholeSaleWhenAnyLineFails() throws Exception {
        TestVariant enoughStock = createTestVariant(3);
        TestVariant noStock = createTestVariant(0);

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"variantId":%d,"quantity":1},{"variantId":%d,"quantity":1}]}
                                """.formatted(enoughStock.variantId(), noStock.variantId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INSUFFICIENT_STOCK"));

        assertThat(stockOf(enoughStock.variantId())).isEqualTo(3);
        assertThat(saleMovementsOf(enoughStock.variantId())).isZero();
        assertThat(saleMovementsOf(noStock.variantId())).isZero();
    }

    @Test
    void rejectsDuplicateVariantInOneSale() throws Exception {
        TestVariant variant = createTestVariant(5);

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"variantId":%d,"quantity":1},{"variantId":%d,"quantity":1}]}
                                """.formatted(variant.variantId(), variant.variantId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsUnknownVariant() throws Exception {
        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"variantId":999999,"quantity":1}]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void rejectsSaleOfVariantBelongingToInactiveItem() throws Exception {
        TestVariant variant = createTestVariant(5);

        mockMvc.perform(delete("/api/v1/items/" + variant.itemId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"variantId":%d,"quantity":1}]}
                                """.formatted(variant.variantId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void replaysIdempotentRequestWithoutDoubleDecrement() throws Exception {
        TestVariant variant = createTestVariant(5);
        String idempotencyKey = UUID.randomUUID().toString();
        String body = """
                {"lines":[{"variantId":%d,"quantity":1}]}
                """.formatted(variant.variantId());

        int firstSaleId = objectMapper.readTree(mockMvc.perform(post("/api/v1/sales")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asInt();

        mockMvc.perform(post("/api/v1/sales")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(firstSaleId));

        assertThat(stockOf(variant.variantId())).isEqualTo(4);
        assertThat(saleMovementsOf(variant.variantId())).isEqualTo(1);
    }

    @Test
    void rejectsSameIdempotencyKeyWithDifferentPayload() throws Exception {
        TestVariant variant = createTestVariant(5);
        String idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/sales")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"variantId":%d,"quantity":1}]}
                                """.formatted(variant.variantId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sales")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lines":[{"variantId":%d,"quantity":2}]}
                                """.formatted(variant.variantId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_REUSED"));
    }


    private int createSale(String body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asInt();
    }

    private TestVariant createTestVariant(int stock) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        String itemResponse = mockMvc.perform(post("/api/v1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"TEST-ITEM-%s","name":"Test item","basePrice":10000}
                                """.formatted(suffix)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int itemId = objectMapper.readTree(itemResponse).get("id").asInt();

        String variantResponse = mockMvc.perform(post("/api/v1/items/" + itemId + "/variants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"TEST-VAR-%s","name":"Only variant","initialStock":%d}
                                """.formatted(suffix, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return new TestVariant(itemId, objectMapper.readTree(variantResponse).get("id").asInt());
    }

    private int stockOf(int variantId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/variants/" + variantId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("stock").asInt();
    }

    private JsonNode firstMovementOf(int variantId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/variants/" + variantId + "/stock-movements"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("content").get(0);
    }

    private long saleMovementsOf(int variantId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/variants/" + variantId + "/stock-movements"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long count = 0;
        for (JsonNode movement : objectMapper.readTree(response).get("content")) {
            if ("SALE".equals(movement.get("type").asText())) {
                count++;
            }
        }
        return count;
    }
}

