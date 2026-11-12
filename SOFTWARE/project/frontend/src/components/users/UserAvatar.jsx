import { Avatar, Typography } from "@mui/material";
import React from "react";
import { AvatarColours } from "../../api/colours";
import theme from "../../styles/theme";

function UserAvatar({
    size = "36px",
    username,
    avatarColour,
    border = false
}) {
    return(
        <Avatar
            sx={{
                width: size, 
                height: size,
                bgcolor: avatarColour ? AvatarColours[avatarColour] : "lightgrey",
                color: "white",
                ...(border ? {border: "1px solid white"} : {})
            }}
        >
            <Typography fontSize={`calc(${size}/1.75)`}>{username ? username[0].toUpperCase() : "-"}</Typography>
        </Avatar>
    );
}

export default React.memo(UserAvatar);