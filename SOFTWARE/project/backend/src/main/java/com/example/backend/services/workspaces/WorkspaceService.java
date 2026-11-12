package com.example.backend.services.workspaces;

import com.example.backend.dto.responses.workspaces.*;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import com.example.backend.model.workspaces.WorkspaceMember;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.repositories.bugs.ReportContainerActivityRepository;
import com.example.backend.repositories.bugs.ReportContainerRepository;
import com.example.backend.repositories.reports.DuplicateSuggestionRepository;
import com.example.backend.repositories.reports.ReportAttachmentRepository;
import com.example.backend.repositories.reports.ReportRepository;
import com.example.backend.repositories.users.UserRepository;
import com.example.backend.repositories.bugs.ReportContainerAssigneeRepository;
import com.example.backend.repositories.workspaces.ReportContainerWorkspaceCustomLabelMappingRepository;
import com.example.backend.repositories.workspaces.ReportWorkspaceCustomLabelMappingRepository;
import com.example.backend.repositories.workspaces.WorkspaceMemberRepository;
import com.example.backend.repositories.workspaces.WorkspaceRepository;
import com.example.backend.dto.requests.workspaces.CustomLabelDTO;
import com.example.backend.dto.requests.workspaces.MemberRoleDTO;
import com.example.backend.services.users.NotificationService;
import com.example.backend.services.users.UserService;
import jakarta.transaction.Transactional;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WorkspaceService {

    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private ReportContainerAssigneeRepository reportContainerAssigneeRepository;
    @Autowired
    private ReportContainerWorkspaceCustomLabelMappingRepository reportContainerWorkspaceCustomLabelMappingRepository;
    @Autowired
    private ReportWorkspaceCustomLabelMappingRepository reportWorkspaceCustomLabelMappingRepository;
    @Autowired
    private ReportContainerRepository reportContainerRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private DuplicateSuggestionRepository duplicateSuggestionRepository;
    @Autowired
    private ReportAttachmentRepository reportAttachmentRepository;
    @Autowired
    private ReportContainerActivityRepository reportContainerActivityRepository;

    private static final Logger log = LoggerFactory.getLogger(WorkspaceService.class);

    /** Valid colours for workspaces. */
    public static final Set<String> WORKSPACE_COLOURS = Set.of(
        "red", "orange", "yellow", "lime", "green", "sky", "blue", "indigo", "violet", "pink"
    );

    /** Valid colours for custom workspace labels. */
    public static final Set<String> CUSTOM_LABEL_COLOURS = Set.of(
        "red", "orange", "yellow", "lime", "green", "sky", "blue", "indigo", "violet", "pink"
    );

    /**
     * Method to check if workspace repository is empty.
     * Useful for loading sample workspaces for testing if none are there.
     * @return true if it is empty, otherwise false.
     */
    public boolean isWorkspaceRepositoryEmpty() {
        return workspaceRepository.count() == 0;
    }

    /** Method to create a new workspace */
    @Transactional
    public Workspace createWorkspace(
        User owner,
        String name,
        String description,
        boolean isPublic,
        String colour,
        Map<User, WorkspaceRole> members,
        Map<String, String> customLabels
    ) {
        //Create new workspace.
        Workspace workspace = new Workspace(name, description, isPublic, colour, owner);

        //Add members to workspace.
        if (members != null) {
            for (Map.Entry<User, WorkspaceRole> entry : members.entrySet()) {
                workspace.getWorkspaceMembers().add(new WorkspaceMember(
                    workspace,
                    entry.getKey(), //User
                    entry.getValue() //Role
                ));
            }
        }

        //Create and add custom labels.
        if (customLabels != null) {
            for (Map.Entry<String, String> customLabel : customLabels.entrySet()) {
                workspace.getWorkspaceCustomLabels().add(new WorkspaceCustomLabel(
                    customLabel.getKey(), //Label Name
                    customLabel.getValue()) //Label Colour
                );
            }
        }

        //Save new workspace.
        workspaceRepository.save(workspace);

        //Return workspace;
        return workspace;
    }

    /**
     * Method to delete a specific workspace.
     * @param workspace The workspace to delete.
     */
    @Transactional
    public void deleteWorkspace(Workspace workspace) {
        //Remove all relevant attachments
        reportContainerWorkspaceCustomLabelMappingRepository.deleteAllByWorkspaceId(workspace.getId());
        reportWorkspaceCustomLabelMappingRepository.deleteAllByWorkspaceId(workspace.getId());
        duplicateSuggestionRepository.deleteAllByWorkspaceId(workspace.getId());
        reportContainerActivityRepository.deleteAllByWorkspaceId(workspace.getId());
        reportContainerAssigneeRepository.deleteAllByWorkspaceId(workspace.getId());
        reportAttachmentRepository.deleteAllByWorkspaceId(workspace.getId());
        reportRepository.breakReportContainerRelationsByWorkspaceId(workspace.getId());
        reportContainerRepository.breakReportRelationsByWorkspaceId(workspace.getId());
        reportRepository.deleteAllByWorkspaceId(workspace.getId());
        reportContainerRepository.deleteAllByWorkspaceId(workspace.getId());
        //Then the workspace
        workspaceRepository.delete(workspace);
    }

    @Transactional
    public Workspace updateWorkspaceName(Workspace workspace, String name){
        workspace.setName(name);
        workspace = workspaceRepository.save(workspace);
        return workspace;
    }

    @Transactional
    public Workspace updateWorkspaceVisibility(Workspace workspace, Boolean visibility) {
        workspace.setPublic(visibility);
        workspace = workspaceRepository.save(workspace);
        return workspace;
    }

    @Transactional
    public Workspace updateWorkspaceColour(Workspace workspace, String colour) {
        workspace.setColour(colour);
        workspace = workspaceRepository.save(workspace);
        return workspace;
    }

    @Transactional
    public Workspace updateWorkspaceDescription(Workspace workspace, String description) {
        workspace.setDescription(description);
        workspace = workspaceRepository.save(workspace);
        return workspace;
    }

    @Transactional
    public void addCustomWorkspaceLabelToWorkspace(Workspace workspace, String labelName, String labelColour) {
        workspace.getWorkspaceCustomLabels().add(new WorkspaceCustomLabel(labelName, labelColour));
        workspaceRepository.save(workspace);
    }

    @Transactional
    public void removeCustomWorkspaceLabelFromWorkspace(Workspace workspace, String labelName) {
        WorkspaceCustomLabel wcl = findCustomWorkspaceLabelInWorkspace(workspace, labelName);
        if (wcl == null) return;
        //Delete all mappings of workspace custom label before deleting label, to avoid foreign key issues.
        reportContainerWorkspaceCustomLabelMappingRepository.deleteAllByWorkspaceCustomLabelId(wcl.getId());
        reportWorkspaceCustomLabelMappingRepository.deleteAllByWorkspaceCustomLabelId(wcl.getId());
        //Delete the workspace custom label by removing it from the workspace list of labels. It will then be deleted by orphan removal.
        workspace.getWorkspaceCustomLabels().remove(wcl);
        workspaceRepository.save(workspace);
    }

    @Transactional
    public void updateCustomWorkspaceLabelColourInWorkspace(Workspace workspace, WorkspaceCustomLabel wcl, String labelColour) {
        wcl.setColour(labelColour);
        workspaceRepository.save(workspace);
    }

    @Transactional
    public void addWorkspaceMemberToWorkspace(Workspace workspace, User user, WorkspaceRole role) {
        workspace.getWorkspaceMembers().add(new WorkspaceMember(workspace, user, role));
        workspaceRepository.save(workspace);
    }

    @Transactional
    public void removeWorkspaceMemberFromWorkspace(Workspace workspace, User user) {
        WorkspaceMember wm = findWorkspaceMemberInWorkspace(workspace, user);
        reportContainerAssigneeRepository.deleteByWorkspaceMemberId(wm.getId());
        workspace.getWorkspaceMembers().remove(wm);
        workspaceRepository.save(workspace);
    }

    @Transactional
    public void updateWorkspaceMemberRoleInWorkspace(Workspace workspace, WorkspaceMember wm, WorkspaceRole role) {
        wm.setRole(role);
        workspaceRepository.save(workspace);
    }

    /* ---------- Query for DTO Service Methods ---------- */

    /**
     * Method to find owned workspace summary DTOs.
     * @param owner The user to find for
     * @return A list of owned workspace overview DTOs.
     */
    public List<OwnedWorkspaceOverviewDTO> findWorkspaceSummariesByOwnerAsDTO(User owner) {
        return workspaceRepository.findWorkspaceSummariesByOwnerAsDTO(owner);
    }

    /**
     * Method to find member workspace summary DTOs.
     * @param member The user to find for
     * @return A list of member workspace overview DTOs.
     */
    public List<MemberWorkspaceOverviewDTO> findWorkspaceSummariesByMemberAsDTO(User member) {
        return workspaceMemberRepository.findWorkspaceSummariesByMemberAsDTO(member);
    }

    /**
     * Method to find workspace summary DTOs for each workspace the user belongs to.
     * @param user The user to find for
     * @return A list of workspace overview DTOs.
     */
    public List<ShortWorkspaceOverviewDTO> findShortWorkspaceOverviewByUserAsDTO(User user) {
        return workspaceMemberRepository.findShortWorkspaceSummariesByUserAsDTO(user);
    }

    /**
     * Method to find workspace members in DTO format.
     * @param workspace The workspace to find the members of.
     * @return A list of workspace member DTOs.
     */
    public List<WorkspaceMemberDTO> findWorkspaceMembersAsDTO(Workspace workspace) {
        return workspaceMemberRepository.findWorkspaceMembersByWorkspaceAsDTO(workspace);
    }

    /**
     * Method to find assignable workspace members in DTO format.
     * @param workspace The workspace to find the assignable members of.
     * @return A list of workspace member DTOs.
     */
    public List<WorkspaceMemberDTO> findAssignableWorkspaceMembersAsDTO(Workspace workspace) {
        return workspaceMemberRepository.findAssignableWorkspaceMembersByWorkspaceAsDTO(
            workspace,
            List.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN, WorkspaceRole.DEVELOPER)
        );
    }

    /**
     * Method to find workspace members in DTO format.
     * @param workspace The workspace to find the members of.
     * @return A list of workspace member DTOs.
     */
    public List<WorkspaceCustomLabelDTO> findWorkspaceCustomLabelsAsDTO(Workspace workspace) {
        return workspaceRepository.findWorkspaceCustomLabelsByWorkspace(workspace);
    }

    /* ---------- Query Service Methods ---------- */

    /**
     * Method to find workspace by its id.
     * @param id The id of the workspace to find
     * @return the workspace if it exists.
     */
    public Optional<Workspace> findWorkspaceById(Long id) {
        return workspaceRepository.findById(id);
    }

    public Optional<Workspace> findWorkspaceByIdWithCustomLabels(Long id) {
        return workspaceRepository.findByIdWithCustomLabels(id);
    }

    /**
     * Method to find the users role in a workspace.
     * @param workspace The workspace to check the role in.
     * @param currentUser The current user to check the role of.
     * @return The users WorkspaceRole in the workspace, otherwise null if they aren't a member.
     */
    public WorkspaceRole findUserRoleInWorkspace(Workspace workspace, User currentUser) {
        return workspaceMemberRepository.findRoleByUserAndWorkspace(currentUser, workspace).orElse(null);
    }

    /**
     * Method to check if a user is a member of a workspace
     * @param user The user
     * @param workspace The workspace
     * @return true if they are, otherwise false.
     */
    public boolean isUserWorkspaceMember(User user, Workspace workspace) {
        return workspaceMemberRepository.existsByUserAndWorkspace(user, workspace);
    }

    /**
     * Method to fetch the workspace custom label object if it exists, otherwise to return null.
     * @param workspace The workspace
     * @param labelName The unique label name
     * @return The workspace custom label object if it exists, otherwise null.
     */
    public WorkspaceCustomLabel findCustomWorkspaceLabelInWorkspace(Workspace workspace, String labelName) {
        return workspaceRepository.findWorkspaceCustomLabelByWorkspaceAndWorkspaceCustomLabelName(workspace, labelName).orElse(null);
    }

    /**
     * Method to fetch the workspace member object if it exists, otherwise to return null.
     * @param workspace The workspace
     * @param member The user member
     * @return The workspace member object if it exists, otherwise null.
     */
    public WorkspaceMember findWorkspaceMemberInWorkspace(Workspace workspace, User member) {
        return workspaceMemberRepository.findByUserAndWorkspace(member, workspace).orElse(null);
    }

    /**
     * Method the fetch the workspace member object if it exists, otherwise to return null.
     * @param workspace The workspace.
     * @param userId The user ID of the user to find the workspace membership object for.
     * @return The workspace member object if it exists, otherwise null.
     */
    public WorkspaceMember findWorkspaceMemberInWorkspaceByUserId(Workspace workspace, Long userId) {
        return workspaceMemberRepository.findByUserIdAndWorkspace(userId, workspace.getId()).orElse(null);
    }

    public List<User> findWorkspaceMemberUsersByWorkspaceAndNotCurrentUser(Workspace workspace, User currentUser) {
        return workspaceMemberRepository.findByWorkspaceIdAndUserIdNot(workspace.getId(), currentUser.getId());
    }

    /**
     * Method to check that the workspace custom label has not been already added to the workspace.
     * @return true if it already exists, otherwise false.
     */
    public boolean doesWorkspaceCustomLabelExist(String name, Workspace workspace) {
        return workspaceRepository.existsByIdAndWorkspaceCustomLabelsName(workspace.getId(), name);
    }

    /* ---------- Workspace Validation Methods ---------- */

    /**
     * Method to validate workspace name:
     * - No special characters other than spaces, - or _ allowed.
     * - At least 1 letter.
     * - Min 3 characters.
     * - Max 30 characters.
     * - A single space is allowed.
     * @return true if valid, otherwise false.
     */
    public boolean isWorkspaceNameValid(String name) {
        log.info("Entered Workspace Name Validation");
        //Presence check
        if (name == null || name.isBlank()) { return false; }
        //Format and length check
        //(look ahead to check at least 1 character is letter, only supported letters, and valid length)
        return name.matches("^(?=.*[A-Za-z])[A-Za-z0-9_\\- ]{3,30}$");
    }

    /**
     * Method to validate workspace description:
     * This is not required but can only be a maximum of 500 characters if provided.
     * @return true if valid, otherwise false.
     */
    public boolean isWorkspaceDescriptionValid(String description) {
        log.info("Entered Workspace Description Validation");
        //Still valid if no description as this is not required.
        if (description == null || description.isBlank()) { return true; }
        //Validate length.
        return description.length() <= 500;
    }

    /**
     * Method to validate that workspace colour is a valid colour from colour collection.
     * @param colour The colour the check if it is valid, must not be null.
     * @return true if valid, otherwise false.
     */
    public boolean isWorkspaceColourValid(String colour) {
        log.info("Entered Workspace Colour Validation");
        return WORKSPACE_COLOURS.contains(colour);
    }

    /**
     * Method to validate workspace custom label name.
     * - No special characters apart from space, - or _ allowed.
     * - Min 1 character.
     * - Max 20 characters.
     * @return true if valid, otherwise false.
     */
    public boolean isWorkspaceCustomLabelNameValid(String name) {
        log.info("Entered Workspace Custom Label Name Validation");
        //Presence check
        if (name == null || name.isBlank()) { return false; }
        //Format and length check (only supported letters, and valid length)
        return name.matches("^[A-Za-z0-9_\\- ]{1,20}$");
    }

    /**
     * Method to validate that workspace custom label colour is a valid colour from colour collection.
     * @return true if valid, otherwise false.
     */
    public boolean isWorkspaceCustomLabelColourValid(String colour) {
        log.info("Entered Workspace Custom Label Colour Validation");
        return CUSTOM_LABEL_COLOURS.contains(colour);
    }

    /** Method to check if workspace member name is in the right format */
    public boolean isWorkspaceMemberNameFormatValid(String name) {
        log.info("Entered Workspace Workspace Member Name Format Validation");
        return userService.isValidUsername(name);
    }

    /** Method to get while checking if a workspace member role string is valid. */
    public WorkspaceRole getValidWorkspaceMemberRole(String role) {
        try {
            return WorkspaceRole.valueOf(role.toUpperCase());
        } catch (Exception e) {
            log.info("An invalid workspace role was provided.");
            return null;
        }
    }

    /** Method to convert CustomLabelsDTO list into a map */
    public Map<String, String> validateCustomLabelsDtoToMap(List<CustomLabelDTO> labelsDTO) throws BadRequestException {
        log.info("Entered Workspace Custom Label DTO Validation");

        Map<String, String> customLabels = new HashMap<>();
        String labelName;
        String labelColour;
        boolean valid;
        for (CustomLabelDTO customLabelDTO : labelsDTO) {
            labelName = customLabelDTO.name();
            labelColour = customLabelDTO.colour();
            //Presence check
            if (labelName == null || labelColour == null || labelName.isBlank() || labelColour.isBlank()) {
                continue;
            }
            //Sanitise
            labelName = labelName.trim().replaceAll("\\s+", " ");
            labelColour = labelColour.trim();
            //Validate
            valid = isWorkspaceCustomLabelNameValid(labelName);
            valid = valid && isWorkspaceCustomLabelColourValid(labelColour);
            if (!valid) {
                throw new BadRequestException("An invalid custom workspace label was provided.");
            }
            //Add to valid labels map
            customLabels.put(labelName, labelColour);
        }
        return customLabels;
    }

    /** Method to convert MemberRoleDTO list into a valid map */
    public Map<User, WorkspaceRole> validateMembersDtoToMap(List<MemberRoleDTO> membersDTO, User currentUser) throws BadRequestException {
        log.info("Entered Workspace Members DTO Validation");

        Map<User, WorkspaceRole> members = new HashMap<>();
        User memberUser;
        WorkspaceRole memberRole;
        String tempMember;
        String tempRole;
        for (MemberRoleDTO memberRoleDTO : membersDTO) {
            tempMember = memberRoleDTO.username();
            tempRole = memberRoleDTO.role();
            //Presence check
            if (tempMember == null || tempMember.isBlank() || tempRole == null || tempRole.isBlank()) {
                continue;
            }
            //Validate format
            if (!isWorkspaceMemberNameFormatValid(tempMember)) throw new BadRequestException();
            //Check if user exists
            memberUser = userService.findUserByUsername(tempMember.trim());
            if (memberUser == null) throw new BadRequestException();
            //Ensure they are not trying to self add.
            if (memberUser == currentUser) throw new BadRequestException();
            //Validate workspace role.
            memberRole = getValidWorkspaceMemberRole(tempRole);
            if (memberRole == null) throw new BadRequestException();
            //Add member to valid workspace members map.
            members.put(memberUser, memberRole);
        }
        return members;
    }

    /** Method to get while checking if workspace custom label is valid */
    public WorkspaceCustomLabel getValidWorkspaceCustomLabel(Workspace workspace, String labelName) {
        if (isWorkspaceCustomLabelNameValid(labelName)) {
            return findCustomWorkspaceLabelInWorkspace(workspace, labelName);
        } else {
            return null;
        }
    }

    /**
     * Method to get and validate a list of string labels into a set of workspace available labels.
     * @param workspace The workspace to check in
     * @param labels The list of incoming labels to check and find
     * @return The set of corresponding workspace custom labels.
     */
    public Set<WorkspaceCustomLabel> getValidWorkspaceCustomLabelsSet(Workspace workspace, List<String> labels) {
        Set<WorkspaceCustomLabel> labelsCollection = new HashSet<>();
        WorkspaceCustomLabel aLabel;
        for (String labelName : labels) {
            aLabel = getValidWorkspaceCustomLabel(workspace, labelName);
            if (aLabel != null) {
                labelsCollection.add(aLabel);
            }
        }
        return labelsCollection;
    }

}
