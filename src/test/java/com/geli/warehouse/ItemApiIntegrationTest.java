package com.geli.warehouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ItemApiIntegrationTest {
    private static final String DATABASE_URL =
            "jdbc:sqlite:./target/test-item-api-" + UUID.randomUUID()
            + ".db?foreign_keys=on&busy_timeout=1000&journal_mode=WAL&transaction_mode=IMMEDIATE";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", () -> DATABASE_URL);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsItemAndReturnsLocation() throws Exception{
        mockMvc.perform(post("/api/v1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"sku": "NEW-ITEM", "name": "New Item", "description": "from test", "basePrice": 10000}
                """)
        ).andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.sku").value("NEW-ITEM"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.variantCount").value(0));
    }

    @Test
    void rejectsDuplicateSkuWith409() throws Exception{
        mockMvc.perform(post("/api/v1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"sku": "TSHIRT-BASIC", "name": "Duplicate", "basePrice": 10000}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));

    }

    @Test
    void rejectsInvalidPayloadWith400AndFieldDetails() throws Exception{
        mockMvc.perform(post("/api/v1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"sku": "", "name": "", "basePrice": -5}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details").isNotEmpty());
    }

    @Test
    void returnItemWithAggregatedVariantStats() throws Exception {
        mockMvc.perform(get("/api/v1/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TSHIRT-BASIC"))
                .andExpect(jsonPath("$.variantCount").value(2))
                .andExpect(jsonPath("$.totalStock").value(35));
    }

    @Test
    void returns404ForUnknownItem() throws Exception {
        mockMvc.perform(get("/api/v1/items/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void updatesItem() throws Exception{
        mockMvc.perform(put("/api/v1/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"name": "Updated Tee", "description": "updated", "basePrice": 80000}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Tee"))
                .andExpect(jsonPath("$.basePrice").value(80000));
    }

    @Test
    void softDeletesItemAndHidesItFromDefaultListing() throws Exception{
        mockMvc.perform(delete("/api/v1/items/3"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/items/3"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/items").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("CAP-DENIM"));
    }

    @Test
    void searchesItemsByName() throws Exception{
        mockMvc.perform(get("/api/v1/items").param("q", "shoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("SNEAKER-RUN"));
    }
}
