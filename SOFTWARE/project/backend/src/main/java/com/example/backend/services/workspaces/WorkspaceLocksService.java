package com.example.backend.services.workspaces;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class WorkspaceLocksService {

    private final ConcurrentHashMap<Long, ReentrantLock> workspaceResolveDuplicatesLock = new ConcurrentHashMap<>();

    /**
     * Method to get a resolve duplicates lock for a specific workspace, or create one if it doesn't exist yet.
     * Each workspace can have one resolve duplicates lock, denoted by the workspace ID.
     * @param workspaceId The workspace ID.
     * @return the ReentrantLock.
     */
    public ReentrantLock getResolveDuplicatesLockByWorkspace(long workspaceId) {
        return workspaceResolveDuplicatesLock.computeIfAbsent(workspaceId, id -> new ReentrantLock(true));
    }
}
