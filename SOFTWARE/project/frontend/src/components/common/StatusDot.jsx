import React from "react";

function StatusDot({
    colour, 
    size = "8px"
}) {
    return (<>
        <span style={{
            display: "inline-block",
            width: size,
            height: size,
            backgroundColor: colour,
            borderRadius: "50%",
            marginRight: "8px",
            flexShrink: 0
        }}/>
    </>);
}

export default React.memo(StatusDot);