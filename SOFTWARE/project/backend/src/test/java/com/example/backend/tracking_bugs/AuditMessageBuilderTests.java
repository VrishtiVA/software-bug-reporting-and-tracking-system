package com.example.backend.tracking_bugs;

import com.example.backend.model.bugs.AuditToken;
import com.example.backend.model.bugs.AuditTokenType;
import com.example.backend.services.bugs.AuditMessageBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
public class AuditMessageBuilderTests {

    @Test
    void testAuditMessageBuilderSoloBuildText() {
        List<AuditToken> response = new AuditMessageBuilder().text("hello").build();
        assertEquals(1, response.size());
        assertEquals(AuditTokenType.TEXT, response.getFirst().getType());
        assertEquals("hello", response.getFirst().getValue());
    }

    @Test
    void testAuditMessageBuilderSoloBuildUsername() {
        List<AuditToken> response = new AuditMessageBuilder().username("va104").build();
        assertEquals(1, response.size());
        assertEquals(AuditTokenType.USERNAME, response.getFirst().getType());
        assertEquals("va104", response.getFirst().getValue());
    }

    @Test
    void testAuditMessageBuilderSoloBuildOldValue() {
        List<AuditToken> response = new AuditMessageBuilder().oldValue("old").build();
        assertEquals(1, response.size());
        assertEquals(AuditTokenType.OLD_VALUE, response.getFirst().getType());
        assertEquals("old", response.getFirst().getValue());
    }

    @Test
    void testAuditMessageBuilderSoloBuildNewValue() {
        List<AuditToken> response = new AuditMessageBuilder().newValue("new").build();
        assertEquals(1, response.size());
        assertEquals(AuditTokenType.NEW_VALUE, response.getFirst().getType());
        assertEquals("new", response.getFirst().getValue());
    }

    @Test
    void testAuditMessageBuilderSoloBuildId() {
        List<AuditToken> response = new AuditMessageBuilder().id(1L).build();
        assertEquals(1, response.size());
        assertEquals(AuditTokenType.ID, response.getFirst().getType());
        assertEquals("1", response.getFirst().getValue());
    }

    @Test
    void testAuditMessageBuilderAssembly() {
        List<AuditToken> response = new AuditMessageBuilder()
                .text("hello")
                .username("va104")
                .id(1L)
                .oldValue("old")
                .text("to")
                .newValue("new")
                .build();
        assertEquals(6, response.size());
        assertEquals(AuditTokenType.TEXT, response.getFirst().getType());
        assertEquals("hello", response.getFirst().getValue());
        assertEquals(AuditTokenType.USERNAME, response.get(1).getType());
        assertEquals("va104", response.get(1).getValue());
        assertEquals(AuditTokenType.ID, response.get(2).getType());
        assertEquals("1", response.get(2).getValue());
        assertEquals(AuditTokenType.OLD_VALUE, response.get(3).getType());
        assertEquals("old", response.get(3).getValue());
        assertEquals(AuditTokenType.TEXT, response.get(4).getType());
        assertEquals(AuditTokenType.NEW_VALUE, response.get(5).getType());
        assertEquals("new", response.get(5).getValue());
    }

}
