
# Duplicate Detection Lifecycle Flow Diagram

> This is my flow design for using duplicate detection to contain bug reports into groups. This would help find up-to-date duplicate suggestions while keeping up with new incoming reports and new containers (changes to the corpus).

```mermaid 
%%{init: { 
  'theme': 'base'
}}%%

flowchart TB
    A(["NEW"]) -- (async) --> n13["Calculate embedding using microservice (async)"]
    n13 --> n1(["READY"])
    n1 --> n14["Check for duplicates against open contained and older created reports using microservice (async)"]
    n14 --> n7{"If duplicate suggestions"}
    n7 -- No --> n20{"Is report primary of a PRELIMINARY container"}
    n7 -- Yes --> n18{"If first suggestion is confident (over upper similarity threshold)"}
    n8["Create new STABLE container and contain the report"] --> n9(["CONTAINED"])
    n18 -- No --> n2(["OUTSTANDING"])
    n2 -- When user is resolving --> n3{"Resolve duplicate suggestion"}
    n3 -- If suggestion rejected --> n5{"Count remaining suggestions"}
    n5 -- If no more suggestions --> n22(["RESOLVED"])
    n22 --> n20
    n20 -- No --> n8
    n20 -- Yes --> n21["Set the container stability to STABLE and update status of all reports in the container to contained"]
    n21 --> n9
    n5 -- If more suggestions --> n3
    n3 -- If suggestion is approved --> n23(["RESOLVED"])
    n23 --> n10{"If suggested report is a member of a report container (with any stability)"}
    n18 -- Yes --> n10
    n10 -- Yes --> n11{"Check report container stability state"}
    n11 -- Is STABLE --> n4["Contain the report (as well as the members of its PRELIMINARY container, if its the primary of one) in the STABLE container"]
    n11 -- Is PRELIMINARY --> n15["Add the report (as well as the members of its PRELIMINARY container, if its the primary of one) to the PRELIMINARY container"]
    n4 --> n9
    n9 --> n12["Clear duplicate suggestions and PRELIMINARY containers from the report if any"]
    n10 -- No --> n16["Create a PRELIMINARY container to hold the reports together (as well as the members of the initial reports PRELIMINARY container, if its the primary of one), set the suggested report as its primary report, and mark the initial as STAGED."]
    n15 --> n17(["STAGED"])
    n16 --> n17
```