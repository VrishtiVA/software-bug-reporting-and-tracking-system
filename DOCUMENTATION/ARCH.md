
# High Level System Architecture Diagram

> This file contains a __High-Level System Architecture Design Diagram__ for this project. Note: Since GitLab only supports an older version of Mermaid, the configuration of this diagram is adapted for it to appear on GitLab. The original diagram was included in the Interim Report.

```mermaid
%%{init: { 
  'theme': 'base'
}}%%

flowchart TB

  subgraph s1["react-frontend"]
    n1["UI Components"]
  end
  subgraph s2["spring-backend"]
    n2["RESTControllers"]
    n3["Services"]
    n4["Data Repositories (JPA)"]
  end
  subgraph s3["fastapi-service"]
    n6["AI / Duplicate Detection Microservices"]
  end
  n1 <-. REST API Requests and Responses using AXIOS .-> n2
  n2 <-- " " --> n3
  n3 <-- " " --> n4
  n4 <-- Read/Write Access --> n5["MySQL Database"]
  n6 <-. Requests and Responds (Async) .-> n3
  n3 <-- Store/Retrieve Attachment Files --> n7["File Storage"]
  n5 <-- "Read-Only Access" --> n6

%%  n5@{ shape: cyl}
%%  n7@{ shape: stored-data}
```

## Bulked Up Version for Dissertation
Due to GitLab rendering limitations for mermaid, this may look a bit sad, but it looks better in the dissertation so don't worry. This file mainly serves the purpose of saving the chart source code.

```mermaid
%%{init: { 
  'theme': 'neutral'
}}%%

flowchart TB
  subgraph s1["react-frontend"]
    n1["Contexts"]
    n8["Axios Interceptor"]
    n12["Routes"]
    n13["Page Layouts"]
    n14["Pages"]
    n15["Theme"]
    n16["Components"]
  end
  subgraph s2["spring-backend"]
    n10["Data Transfer Objects (DTO)"]
    n2["REST Controllers"]
    n3["Services"]
    n9["Model"]
    n4["DAO / Data Repositories (JPA)"]
  end
  subgraph s3["fastapi-duplicate-detection-microservice"]
    n6["DAO / Database Services"]
    n17["Data Transfer Objects (DTO) using Pydantic"]
    n18["Routes"]
    n19["Services"]
    n20["Settings"]
    n21["Transformer Model"]
  end
  n2 <--> n3 & n10
  n3 <-- Store/Retrieve Attachment Files --> n7["Attachment Storage"]
  n1 --> n12
  n3 -- Search, Sort, Filter --> n11["Apache Lucene Search Index"]
  n8 <-. REST API Requests and Responses via Axios, guarded by Spring Security .-> n10
  n12 --> n13
  n14 <--> n8
  n3 <--> n4
  n2 -. "Server-sent event notifications" .-> n1
  n13 --> n14 & n16
  n15 --> n1
  n14 --> n16
  n17 <-. Requests and Responds (Async)<br>via Spring reactive WebClient .-> n3
  n17 <--> n18
  n4 <-- Spring JPA (Read/Write Access) --> n5["MySQL Database"]
  n5 <-- "SQLAlchemy<br>(Read-Only Access)" --> n6
  n6 <--> n19
  n18 <--> n19
  n20 -.-> n19 & n21
  n21 --> n19

%%  n10@{ shape: lean-l}
%%  n17@{ shape: lean-l}
%%  n20@{ shape: doc}
%%  n7@{ shape: stored-data}
%%  n11@{ shape: stored-data}
%%  n5@{ shape: cyl}
```