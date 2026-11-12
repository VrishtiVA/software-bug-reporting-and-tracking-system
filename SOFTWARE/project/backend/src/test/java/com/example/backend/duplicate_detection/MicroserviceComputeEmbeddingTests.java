package com.example.backend.duplicate_detection;

import com.example.backend.TestSampleInitialiserService;
import com.example.backend.dto.responses.duplicate_detection.ReportVectorEmbeddingResponse;
import com.example.backend.model.reports.Report;
import com.example.backend.services.microservices.DuplicateDetectionService;
import com.example.backend.services.reports.ReportService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspaceService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests in this test class require the FastAPI Microservice to be running.
 */
@SpringBootTest
@ActiveProfiles({"test", "h2"}) //Use both application-test and application-test-h2.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MicroserviceComputeEmbeddingTests {

    @Autowired
    private UserService userService;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private DuplicateDetectionService duplicateDetectionService;
    @Autowired
    private TestSampleInitialiserService sampleInitialiserService;

    @BeforeAll
    void setUpFirst() {
        //Initialise sample users
        if (userService.isUserRepositoryEmpty()) {
            sampleInitialiserService.initialiseSampleUser();
            sampleInitialiserService.initialiseDemoUsers();
        }
        //Initialise sample workspace.
        if (workspaceService.isWorkspaceRepositoryEmpty()) {
            sampleInitialiserService.initialiseSampleWorkspace();
            sampleInitialiserService.initialiseSampleReport(false);
            sampleInitialiserService.initialiseAdditionalSampleReports(false);
        }
    }

    /* ---------- Testing @Async CompletableFuture<ReportVectorEmbeddingResponse> computeVectorEmbedding(Report report) ---------- */

    @Test
    void testComputeVectorEmbedding_ReturnsResponse() throws Exception {
        Report report = reportService.findById(1L);
        ReportVectorEmbeddingResponse vectorEmbeddingResponse = duplicateDetectionService.computeVectorEmbedding(report).join();
        assertNotNull(vectorEmbeddingResponse);
        assertNotEquals(null, vectorEmbeddingResponse.vectorEmbedding());
        assertNotEquals("", vectorEmbeddingResponse.vectorEmbedding());
    }

    @Test
    void testComputeVectorEmbedding_MultipleReports() throws Exception {
        //Report 1
        Report report = reportService.findById(2L);
        ReportVectorEmbeddingResponse vectorEmbeddingResponse = duplicateDetectionService.computeVectorEmbedding(report).join();
        assertNotNull(vectorEmbeddingResponse);
        assertNotEquals(null, vectorEmbeddingResponse.vectorEmbedding());
        assertNotEquals("", vectorEmbeddingResponse.vectorEmbedding());
        //Report 2
        report = reportService.findById(3L);
        vectorEmbeddingResponse = duplicateDetectionService.computeVectorEmbedding(report).join();
        assertNotNull(vectorEmbeddingResponse);
        assertNotEquals(null, vectorEmbeddingResponse.vectorEmbedding());
        assertNotEquals("", vectorEmbeddingResponse.vectorEmbedding());
    }

}
