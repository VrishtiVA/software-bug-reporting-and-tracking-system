package com.example.backend;

import com.example.backend.services.SampleInitialiserService;
import com.example.backend.services.bugs.ReportContainerSearchService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class BackendApplication implements ApplicationRunner {

	@Autowired
	private Environment environment;
	@Autowired
	private ReportContainerSearchService reportContainerSearchService;
	@Autowired
	private UserService userService;
    @Autowired
    private WorkspaceService workspaceService;
	@Autowired
	private SampleInitialiserService sampleInitialiserService;

	private static final Logger log = LoggerFactory.getLogger(BackendApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	/**
	 * Initialise some objects on project start running.
	 * Do not load samples here for automated testing,
	 * only loading here for development, manual tests, and demonstrations.
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {

		//Initialise search index.
		reportContainerSearchService.buildSearchIndex();

		//Don't continue here during automated testing.
		if (environment.acceptsProfiles(Profiles.of("test"))) {
			log.info("In test mode, not running backend application class.");
			return;
		}

		/* ----- Initialise demonstration samples ----- */
		log.warn("Continuing to initialise samples in backend application.");

		//Initialise sample users
		if (userService.isUserRepositoryEmpty()) {
			sampleInitialiserService.initialiseSampleUser();
			sampleInitialiserService.initialiseDemoUsers();
			sampleInitialiserService.initialiseTestUser();
		}
		//Initialise sample workspace.
		if (workspaceService.isWorkspaceRepositoryEmpty()) {
			sampleInitialiserService.initialiseSampleWorkspace();
			sampleInitialiserService.initialiseSampleReport(true);
			sampleInitialiserService.initialiseAdditionalSampleReports(true);
		}

	}
}
