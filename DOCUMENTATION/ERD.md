# Entity Relationship Diagram

> This file contains the __Entity Relationship Diagram (ERD)__ for this project, originally created as part of the Interim Report. Note: Since GitLab only supports an older version of Mermaid, the configuration of this diagram is adapted for it to appear on GitLab. The original ERD diagram was included in the Interim Report.

```mermaid 
%%{init: { 
	'layout':'elk',
	'sequence': {'useMaxWidth': false}
}}%%

erDiagram
%%	direction LR

    Notification ||--o{ UserNotificationMapping : "sent to"
    User ||--o{ UserNotificationMapping : "receives"

    User ||--o{ Workspace : "owns 0..5"
    User ||--o{ WorkspaceMembers : "member of"
	Workspace ||--o{ WorkspaceMembers : "has"

	Workspace ||--o{ CustomLabel : "defines"
	CustomLabel ||--o{ CustomLabelMapping : "used by"
	ReportContainer ||--o{ CustomLabelMapping : "uses"

    User ||--o{ Report : "makes"
    Workspace ||--o{ ReportContainer : "contains"
    ReportContainer ||--|{ Report : "contains"
    Report ||--o{ ReportAttachment : "contains"

    Report ||--o{ DuplicateSuggestion : "has"
    ReportContainer ||--o{ DuplicateSuggestion : "given"
	
	ReportContainer ||--o{ ReportContainerAllocation : "handled by"
	User ||--o{ ReportContainerAllocation : "allocated to"

    User ||--o{ ReportContainerActivity : "actions"
    ReportContainer ||--o{ ReportContainerActivity : "has"
    
    ReportContainer ||--o{ ReportContainerWatchMapping : "watched by"
    User ||--o{ ReportContainerWatchMapping : "watches"
    

	User {
		Long id PK
		String username
		String password
		String email
		Enum colour
	}

    UserNotificationMapping {
        Long notificationId PK "FK"
        Long userId PK "FK"
    }

    Notification {
        Long id PK
        String content
        Timestamp timestamp
    }

	Workspace {
		Long id PK
		String title
		String description
		Boolean public
		Enum colour
		Long creatorId "FK"
	}

	WorkspaceMembers {
		Long workspaceId PK "FK"
		Long userId PK "FK"
		Enum role
	}

	CustomLabel {
		Int id PK "FK"
		Long workspaceId PK "FK"
		String name
		Enum colour
	}

	CustomLabelMapping {
		Long reportContainerId PK "FK"
		Long customLabelId PK "FK"
		Long workspaceId PK "FK"
	}

	Report {
		Long id PK
		Long workspaceId "FK"
		Long userId "FK"
		Enum reporterRole
		String summaryTitle
		String stepsToReproduce
		String expectedResult
		String actualResult
		String environment
        String additionalComments
		String fixSuggestions
		Integer severityRating
		Integer userImpactRating
		Timestamp creationTimestamp
        Blob vectorEmbedding
    }

    ReportAttachment {
        Long id PK
        Long reportId "FK"
        String fileReference
        Enum attachmentType
		String caption
    }

    DuplicateSuggestion {
        Long reportId PK "FK"
        Long reportContainerId PK "FK"
        Integer order
        Double similarityScore
    }

    ReportContainer {
        Long id PK
        Long workspaceId "FK"
		String summary
		Float priority
		Enum progress
		Enum status
        Long primaryReport "FK"
    }

	ReportContainerAllocation {
		Long reportContainerId PK "FK"
		Long userId PK "FK"
	}

    ReportContainerWatchMapping {
        Long reportContainerId PK "FK"
        Long userId PK "FK"
    }

	ReportContainerActivity {
        Long id PK
        Long userId "FK"
		Enum userRole
        Long reportContainerId "FK"
        String content
        Enum type
        Timestamp timestamp
    }
```

## Updated Diagram for Dissertation

```mermaid
%%{init: { 
	'layout':'elk',
	'sequence': {'useMaxWidth': false}
}}%%

erDiagram
%%	direction LR
	Notification {
		Long id PK "" 
		Instant timestamp ""
		NotificationType(Enum) type ""
		Long workspaceId ""
		ActionResource(Enum) resource ""
		Long resourceId ""
	}
	NotificationToken {
		Boolean highlight ""
		String value ""
	}
	UserNotificationMapping {
		Long id PK ""
		Long notificationId "FK"  
		Long userId "FK"
		Boolean isRead ""
	}
	User {
		Long id PK ""  
		String username  ""  
		String passwordHash  ""  
		String email  ""  
		String colour  ""  
	}
	Workspace {
		Long id PK ""  
		String name ""  
		String description ""  
		Boolean isPublic ""  
		String colour ""
		Instant creationTimestamp ""
		Long ownerId "FK"  
	}
	WorkspaceMember {
		Long id PK ""
		Long workspaceId "FK"  
		Long userId "FK"  
		WorkspaceRole(Enum) role  ""  
		Long assignmentCount ""
	}
	WorkspaceCustomLabel {
		Int id PK ""
		Long workspaceId "FK"
		String name ""  
		String colour ""
	}
	ReportContainerWorkspaceCustomLabelMapping {
		Long id PK ""
		Long reportContainerId "FK"  
		Long workspaceCustomLabelId "FK"
	}
	ReportContainer {
		Long id PK ""  
		Long workspaceId "FK"  
		String summary ""
		ReportContainerProgress(Enum) progress "" 
		ReportContainerStatus(Enum) status ""
		ReportContainerStability(Enum) stability ""
		Long primaryReportId "FK"
		Instant firstReportTimestamp ""
		Instant lastUpdatedTimestamp ""
		Float priority ""
		Float hotness ""
		Integer frequency ""
		Integer totalSeverityRating ""
		Integer totalUserImpactRating ""
		Integer incomingDuplicateSuggestionsCount ""
	}
	ReportWorkspaceCustomLabelMapping {
		Long id PK ""
		Long reportId "FK"  
		Long workspaceCustomLabelId "FK"
	}
	Report {
		Long id PK ""  
		Long workspaceId "FK"  
		Long userId "FK"  
		Long reportContainerId "FK"
		WorkspaceRole(Enum) userRole ""  
		String summaryTitle ""  
		String stepsToReproduce ""  
		String expectedResult ""  
		String actualResult ""  
		String environment ""  
		String additionalComments ""  
		String fixSuggestions ""  
		Integer severityRating ""  
		Integer userImpactRating ""  
		Instant creationTimestamp ""  
		Blob vectorEmbedding ""
		Instant embeddingTimestamp ""
		DuplicateDetectionLifecycleState(Enum) duplicateDetectionLifecycleState ""
		Boolean duplicateSuggestionsBeingManuallyResolved ""
		Instant duplicateSuggestionsBeingManuallyResolvedTimestamp ""
		Long duplicateSuggestionsBeingManuallyResolvedByUserId ""
	}
	ReportAttachment {
		Long id PK ""
		FileType(Enum) type ""
		String caption ""
		Long size ""
		String reference "" 
		String thumbnailReference ""
	}
	DuplicateSuggestion {
		Long id PK ""
		Long fromReportId "FK"  
		Long toReportId "FK"  
		Integer similarityOrder ""  
		Double similarityScore ""  
	}
	ReportContainerAssignee {
		Long id PK ""
		Long reportContainerId "FK"  
		Long workspaceMemberId "FK"  
	}
	ReportContainerActivity {
		Long id PK ""
		Long reportContainerId "FK" 
		Long userId "FK"
		WorkspaceRole(Enum) performerRole "" 
		Instant timestamp ""
		ReportContainerActivityType(Enum) type ""
		ReportContainerActivityLogType(Enum) logType "" 
		String field ""
	}
	AuditToken {
		AuditTokenType(Enum) type ""
		String value ""
	}
	Notification||--o{UserNotificationMapping:"sent to"
	Notification||--|{NotificationToken:"message made of"
	User||--o{UserNotificationMapping:"receives"
	User||--o{Workspace:"owns 0..5"
	User||--o{WorkspaceMember:"member of"
	Workspace||--o{WorkspaceMember:"has"
	Workspace||--o{WorkspaceCustomLabel:"defines"
	WorkspaceCustomLabel||--o{ReportContainerWorkspaceCustomLabelMapping:"used by"
	ReportContainer||--o{ReportContainerWorkspaceCustomLabelMapping:"uses"
	WorkspaceCustomLabel||--o{ReportWorkspaceCustomLabelMapping:"used by"
	Report||--o{ReportWorkspaceCustomLabelMapping:"uses"
	User||--o{Report:"makes"
	Workspace||--o{ReportContainer:"contains"
	ReportContainer||--|{Report:"contains"
	Report||--o{ReportAttachment:"contains"
	Report||--o{DuplicateSuggestion:"has"
	ReportContainer||--o{ReportContainerAssignee:"handled by"
	WorkspaceMember||--o{ReportContainerAssignee:"allocated to"
	User||--o{ReportContainerActivity:"actions"
	ReportContainer||--o{ReportContainerActivity:"has"
	ReportContainerActivity||--|{AuditToken:"message made of"
```