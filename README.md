# Software Bug Reporting & Tracking System

> This repository holds my BSc Computer Science Dissertation Software Artefact, adapted for viewing on GitHub. The solo-project was *implemented* in feature-based increments between `December 2025` and `April 2026`, originally using GitLab. The dissertation report is not included in this repository.

## Introduction

Software bugs are inevitable, and handling their reports can easily become overwhelming; especially when they arrive from many different users and sources. Some bugs have the potential to disrupt service reliability and/or poison business reputation, making swift action imperative. However, reports often omit the critical information needed to understand and recreate the bug, or describe bugs that have already been reported by others, leading to significant developer time overspent on manual triaging and deciding which bugs to address first. While systems such as Bugzilla and Jira exist to dilute this pressure on developers and organisations, they often have their own frustrations and can fall short in addressing these challenges.

This project aims to develop a web-based software bug reporting and tracking system as a dedicated, structured environment for both technical and non-technical users, developers and organisations. The system will allow users to manage bugs for their own projects within workspaces, providing features including reporting, allocation and progress tracking to support bug lifecycles. To tackle the above challenges, the system will also encourage provision of sufficient detail in reports to assist developers in reproducing and uncovering bugs. Furthermore, the system will provide automated duplicate detection, report-to-bug containerisation, and bug priority calculation to improve triage efficiency and organisation, allowing developers to focus their energy on actual debugging.

## Features

> A simplifed feature list is provided here, while the dissertation defines more specific requirements for each feature to support each development increment, including MoSCoW formatting to outline priorities and support time management.

- __Authentication and User Management__: The ability to sign up, in and out securely, and to be aware of this.
- __Workspace Management__: To allow users to create, customise, and manage their own workspaces and members.
- __Reporting Bugs__: To allow users to report bugs (optionally with attachments) in a specific workspace, while encouraging report quality, adapted to different types of users, to support developers in obtaining relevant details about the issues.
- __Duplicate Detection and Report Containerisation__: To allow the system to automatically detect and group duplicate reports together into a bug within a workspace. If the system is not confident, authorised workspace members can provide confirmations.
- __Assigning Bugs__: Bugs can be assigned to certain members in a workspace.
- __Tracking Bugs__: Bugs and their reports are displayed in a way where the details they provide can be easily utilised. Authorised workspace members can also update labels on bugs to track their progress towards resolution. Edits to bugs are tracked and logged via an audit log.
- __Finding Bugs__: Relevant bugs can be found easily, whether a user is searching for their reports, assignments, highly reported bugs, or bugs with the highest priority for resolution, or simply by search, sorting, and/or filtering.
- __Notifications__: The application provides in-app notifications to users regarding relevant updates, such as their workspace membership changes, assignment related changes, or relevant duplicate report resolutions.

## Techstack

- __Spring Boot__ - To build the core backend to manage application security and data persistance.
- __FastAPI__ - To build a specialised microservice for duplicate detection.
- __React__ - To build the frontend.
- __MaterialUI__ - The UI framework for providing consistent styling and with built-in baseline accessibility.

## Repository Structure

```
va104
├── README.MD
├── PROJECTLOG.MD       <-- Weekly log of project progress      [HIDDEN]
├── DOCUMENTATION/      <-- Holds my mermaid UML diagrams
├── ANALYSIS/           <-- Location for data analysis files    [HIDDEN]
├── TESTING/            <-- Automated test result summaries     [HIDDEN]
├── SOFTWARE/           <-- Location for software artefacts
    ├── project/        <-- Code only beyond here
        ├── frontend/
        ├── backend/
        ├── microservice/
```

## Project Reflections

> Working on this project was a valuable full-stack development experience, allowing me to deepen my understanding of Spring Boot while exploring newer technologies including React and FastAPI. I gained practical experience in designing modular services, implementing asynchronous processing, and considering scalability in application architecture. I was also able to reflect on my architectural decisions to identify opportunities for future improvement and development.

<!-- ## Future Potential

 -->

## Set-Up Instructions
This section provides instructions for a local set-up of the project.

### Database Set-up
Initially you will need to set up a MySQL database that can be used by the backend. 
Please ensure you have downloaded and set up [MySQL Community Server v9.7.0 LTS](https://dev.mysql.com/downloads/mysql/), and downloaded [MySQL Workbench](https://dev.mysql.com/downloads/workbench/).

- Open MySQL Workbench.
- Ensure you have created a database connection.
- Create a new schema called `va104db`.
- Create a new user through `Administration > Users and Privileges`
    - username: `db_user`
    - password: `password`
    - enable all permissions by ticking `DBA` in the `Administrative Roles` tab.

### Microservice Set-up 
Note: Activating the environment steps may be different depending on how your python is and which terminal you are using. I have used `py` and the `git bash` terminal.

- Open the Git Bash terminal.
- Navigate to the `/SOFTWARE/project/microservice` directory.
- Ensure python is installed: `py --version`
- Create environment *within* this microservice folder using: `py -m venv venv`
- Activate environment using: `source venv/Scripts/activate` //Note this command my differ depending on terminal being used.
- Install dependencies listed in `requirements.txt` using: `pip install -r requirements.txt` (may need a while, and may want to enable Windows Long Path support temporarily)

### Backend Set-up
Please ensure you have __Java SDK 21__ and __IntelliJ IDEA__ installed.

- Open `/SOFTWARE/project/backend` within the IDE
- Wait for Gradle dependencies to finish loading.
- Install any other missing drivers if indicated.
- Open `SOFTWARE/project/backend/src/main/resources/application.properties`.
- Link the SQL data-source to the created MySQL database, while ensuring the database connection parameters match the database you created above.

### Frontend Set-up 
Please ensure you have __node__ and __npm__ installed. Node v22.19.0 and npm v11.6.0 were used.

- Open another Git Bash terminal.
- Navigate to the `/SOFTWARE/project/frontend` directory.
- Install dependencies listed in `package.json` using: `npm install` (may need a while)

## Running Instructions 

To run the application, please ensure you have first completed project set-up. 
It is also recommended that you have a charger connected.
Afterwards, you should start up the components in the following order:
1. FastAPI Microservice,
2. Spring Boot Backend,
3. React Frontend

### 1. Running FastAPI Microservice
- Ensure you are in the `/SOFTWARE/project/microservice` directory, and that your Python environment is activated.
- Run using: `uvicorn main:app --reload`
- Give it a moment to start up, until you see `INFO: Application startup complete.` in the command line log.
- The microservice application will then be running on port 8000.

### 2. Running Spring Boot Backend
> Note, the backend application has some sample workspaces, reports, and accounts created that you may wish to use for testing. Otherwise, you can disable this by commenting out `lines 59 to 73` in `SOFTWARE/project/backend/src/main/java/com/example/backend/BackendApplication.java`

- Right-click and run `BackendApplication.java`
- The backend application will then run on port 8080, and you should provide some time for it to set-up until the logs have calmed down.

> If the application didn't run and perhaps complained about unfound data sources, scroll-down to complete the testing set-up before returning here and retrying.

### 3. Running React Frontend
- Ensure you are in the `/SOFTWARE/project/frontend` directory.
- Run project: `npm run dev`
- The frontend application will then be running on port 5173.
- `localhost:5173/` should take you to where you need to be, otherwise you could navigate to `localhost:5173/sign-in`.

#### Sample Accounts If You Want
> If you chose to keep the samples on the backend, here are the existing accounts, but you can create your own too of course.

- username: `va104`, password: `password`
- username: `demonstrator1`, password: `password`
- username: `demonstrator2`, password: `password`
- username: `demonstrator3`, password: `password`

## Running Tests
> Please ensure the project has already been set-up before attempting this section.

### Testing Set-up
You will need to create another MySQL database schema, called `va104db_test`, ensuring that it matches the configuration in `SOFTWARE/project/microservice/duplicate_detection_service/app/database.by`:

- Open MySQL Workbench.
- Ensure you have a database connection.
- Create a new schema called `va104db_test`.
- Open `SOFTWARE/project/backend/src/test/resources/application-test-mysql.properties`.
- Link the SQL data-source to the created MySQL database, while ensuring the database connection parameters match the database you created above.
- Open `SOFTWARE/project/backend/src/test/resources/application-test-h2.properties`.
- Link the H2 data-source through IntelliJ, testing the connection to
    - database name: testdb, 
    - user: sa, 
    - password: none
  (it should be prefilled in the data-source configuration window).

### Running Spring Boot Tests
You will first need to start the FastAPI microservice in test mode, to allow some backend test classes that rely on report containerisation to pass through the microservice for duplicate detection.

- Open Git Bash terminal.
- Ensure you are in the `/SOFTWARE/project/microservice` directory, and that your Python environment is activated.
- Start running the FastAPI microservice with environment variable `test` first. 
    e.g. Linux/Gitbash: `ENV=test uvicorn main:app --reload`. 
- Give it a moment to start up, until you see `INFO: Application startup complete.` in the command line log.
- You can then run all tests in the Spring Boot Backend by running `SOFTWARE/project/backend/src/test/java/com/example/backend`, to execute all test classes. However, it may take some time to run, since test classes are executed sequentially to conduct simulation while avoiding database conflicts.

### Running Python Tests

- Open Git Bash terminal.
- Ensure you are in the `/SOFTWARE/project/microservice` directory, and that your Python environment is activated.
- Run automated tests in python using `python run_tests.py`.

## Lisence

See LICENSE.