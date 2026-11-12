# Structures

## Attachments Folder Structure

```
uploads/
├── workspaces/
    ├── <workspace_id>/
        ├── attachments/
            ├── temporary/
                ├── <temporary_collection_uuid>/
                    ⋮
                ⋮
            ├── permanent/
                ⋮
            ├── thumbnails/
                ⋮
    ⋮
```

## Duplicate Suggestions Response Body Structure

```
{
    from_report_id: int,            <-- The report the suggestion is for
    duplicate_suggestions: [  
        {
            to_report_id: int,          <-- The report the suggestion refers to
            similarity_order: int,      <-- The ranking of the suggestion
            similarity_score: double,   <-- The calculated similarity score
            is_confident: bool          <-- If the score was above the threshold upperbound
        },      
        ⋮
    ]
}
```

## Bug Priority Formula

```
bug_priority = (
    bug_avg_severity * 0.4 +                                        <-- Severity signal
    bug_avg_user_impact * 0.325 +                                   <-- User-impact signal
    log2(bug_frequency) * 0.15 +                                    <-- Frequency signal
    (
        log2(bug_age_weeks +1) *                                        <-- +1 prevents log(0) error
        (((bug_avg_severity + bug_avg_user_impact)/10)**2 + 0.5)        <-- Adaptive time weight
    ) * 0.125                                                       <-- Age signal
) * 20                                                          <-- Enlarge range
```

## Bug Hotness Formula

```
bug_age = bug_age == 0 ? 1 : bug_age        <-- To avoid division by 0 error
bug_hotness = bug_frequency / bug_age
```

## Notification Updates Response Body

```
    {
        unread_count: <long>,         <-- Number of unread notifications
        notification: {               <-- Last unread notification
            id: <long>
            message: {}
            timestamp: <instant>
            ⋮
        }
    } 
```