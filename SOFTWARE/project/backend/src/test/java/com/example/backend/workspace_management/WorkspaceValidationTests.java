package com.example.backend.workspace_management;

import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.services.workspaces.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
public class WorkspaceValidationTests {

    private final WorkspaceService workspaceService = new WorkspaceService();

    /* ---------- Testing workspaceService.isWorkspaceNameValid(String name) ---------- */

    @Test
    void testIsWorkspaceNameValidEmpty() {
        assertFalse(workspaceService.isWorkspaceNameValid(""));
        assertFalse(workspaceService.isWorkspaceNameValid(" "));
        assertFalse(workspaceService.isWorkspaceNameValid("   "));
        assertFalse(workspaceService.isWorkspaceNameValid(null));
    }

    @Test
    void testIsWorkspaceNameValidLength() {
        assertFalse(workspaceService.isWorkspaceNameValid("a"));
        assertTrue(workspaceService.isWorkspaceNameValid("abc"));
        assertTrue(workspaceService.isWorkspaceNameValid("abcdefghijklmnopqrstuvwxyz1234"));
        assertFalse(workspaceService.isWorkspaceNameValid("abcdefghijklmnopqrstuvwxyz12345"));
    }

    @Test
    void testIsWorkspaceNameValidFormat() {
        assertTrue(workspaceService.isWorkspaceNameValid("abc123ABC_-"));
        assertTrue(workspaceService.isWorkspaceNameValid("abc 123 ABC"));
        assertFalse(workspaceService.isWorkspaceNameValid("abc123ABC_-@"));
        assertFalse(workspaceService.isWorkspaceNameValid("abc123$\\ABC_-@"));
        assertTrue(workspaceService.isWorkspaceNameValid("123-ABC"));
        assertFalse(workspaceService.isWorkspaceNameValid("123456"));
        assertFalse(workspaceService.isWorkspaceNameValid("123-456"));
    }

    /* ---------- Testing workspaceService.isWorkspaceDescriptionValid(String description) ---------- */

    @Test
    void testIsWorkspaceDescriptionValidEmpty() {
        assertTrue(workspaceService.isWorkspaceDescriptionValid(""));
        assertTrue(workspaceService.isWorkspaceDescriptionValid(null));
    }

    @Test
    void testIsWorkspaceDescriptionValidLength() {
        assertTrue(workspaceService.isWorkspaceDescriptionValid("abc"));
        assertTrue(workspaceService.isWorkspaceDescriptionValid("1234567890".repeat(50)));
        assertFalse(workspaceService.isWorkspaceDescriptionValid("1234567890".repeat(50).concat("a")));
    }

    /* ---------- Testing workspaceService.isWorkspaceColourValid(String colour) ---------- */

    @Test
    void testIsWorkspaceColourValid() {
        assertFalse(workspaceService.isWorkspaceColourValid(""));
        assertFalse(workspaceService.isWorkspaceColourValid("re"));
        assertTrue(workspaceService.isWorkspaceColourValid("red"));
    }

    /* ---------- Testing workspaceService.isWorkspaceCustomLabelNameValid(String name) ---------- */

    @Test
    void testIsWorkspaceCustomLabelNameValidEmpty() {
        assertFalse(workspaceService.isWorkspaceCustomLabelNameValid(""));
        assertFalse(workspaceService.isWorkspaceCustomLabelNameValid(" "));
        assertFalse(workspaceService.isWorkspaceCustomLabelNameValid("   "));
        assertFalse(workspaceService.isWorkspaceCustomLabelNameValid(null));
    }

    @Test
    void testIsWorkspaceCustomLabelNameValidLength() {
        assertFalse(workspaceService.isWorkspaceCustomLabelNameValid(""));
        assertFalse(workspaceService.isWorkspaceCustomLabelNameValid(" "));
        assertTrue(workspaceService.isWorkspaceCustomLabelNameValid("a"));
        assertTrue(workspaceService.isWorkspaceCustomLabelNameValid("abc"));
        assertTrue(workspaceService.isWorkspaceCustomLabelNameValid("abcdefghijklmnopqrst"));
        assertFalse(workspaceService.isWorkspaceCustomLabelNameValid("abcdefghijklmnopqrstu"));
    }

    @Test
    void testIsWorkspaceCustomLabelNameValidFormat() {
        assertTrue(workspaceService.isWorkspaceCustomLabelNameValid("abc123ABC_-"));
        assertTrue(workspaceService.isWorkspaceCustomLabelNameValid("abc 123 ABC"));
        assertFalse(workspaceService.isWorkspaceCustomLabelNameValid("abc123ABC_-@"));
        assertFalse(workspaceService.isWorkspaceCustomLabelNameValid("abc123$\\ABC_-@"));
        assertTrue(workspaceService.isWorkspaceCustomLabelNameValid("123-ABC"));
        assertTrue(workspaceService.isWorkspaceCustomLabelNameValid("123456"));
        assertTrue(workspaceService.isWorkspaceCustomLabelNameValid("123-456"));
    }

    /* ---------- Testing workspaceService.isWorkspaceCustomLabelColourValid(String colour) ---------- */

    @Test
    void testIsWorkspaceCustomLabelColourValid() {
        assertFalse(workspaceService.isWorkspaceCustomLabelColourValid(""));
        assertFalse(workspaceService.isWorkspaceCustomLabelColourValid("re"));
        assertTrue(workspaceService.isWorkspaceCustomLabelColourValid("red"));
    }

    /* ---------- Testing workspaceService.getValidWorkspaceMemberRole(String role) ---------- */

    @Test
    void testIsWorkspaceMemberRoleValid() {
        assertNull(workspaceService.getValidWorkspaceMemberRole(null));
        assertNull(workspaceService.getValidWorkspaceMemberRole("report"));
        assertEquals(WorkspaceRole.OWNER, workspaceService.getValidWorkspaceMemberRole("owner"));
        assertEquals(WorkspaceRole.ADMIN, workspaceService.getValidWorkspaceMemberRole("admin"));
        assertEquals(WorkspaceRole.DEVELOPER, workspaceService.getValidWorkspaceMemberRole("developer"));
        assertEquals(WorkspaceRole.REPORTER, workspaceService.getValidWorkspaceMemberRole("reporter"));
    }

    @Test
    void testIsWorkspaceMemberRoleInvalid() {
        assertNull(workspaceService.getValidWorkspaceMemberRole(null));
        assertNull(workspaceService.getValidWorkspaceMemberRole(""));
        assertNull(workspaceService.getValidWorkspaceMemberRole("report"));
        assertNull(workspaceService.getValidWorkspaceMemberRole("abc"));
    }

}
