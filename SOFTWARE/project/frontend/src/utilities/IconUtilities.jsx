
import GroupAddIcon from '@mui/icons-material/GroupAddRounded';
import GroupRemoveIcon from '@mui/icons-material/GroupRemoveRounded';
import EditIcon from '@mui/icons-material/EditRounded';
import DeleteIcon from '@mui/icons-material/DeleteOutlineRounded';
import PersonAddIcon from '@mui/icons-material/PersonAddRounded';
import PersonRemoveIcon from '@mui/icons-material/PersonRemoveRounded';
import FileCopyIcon from '@mui/icons-material/FileCopyRounded';
import AwesomeIcon from '@mui/icons-material/AutoAwesomeRounded';
import LightIcon from '@mui/icons-material/WbIncandescentRounded';
import SpeakerIcon from '@mui/icons-material/CampaignRounded';

import { NotificationTypeColours } from '../api/colours';

export const findNotificationIcon = (notificationType, showColour=true) => {
    const iconSettings = showColour ? {sx: {color: NotificationTypeColours[notificationType]}} : {}
    switch (notificationType) {
        case "ADDED_TO_WORKSPACE": return <GroupAddIcon {...iconSettings} />;
        case "REMOVED_FROM_WORKSPACE": return <GroupRemoveIcon {...iconSettings} />
        case "WORKSPACE_UPDATE": return <EditIcon {...iconSettings} />
        case "WORKSPACE_DELETED": return <DeleteIcon {...iconSettings} />
        case "ASSIGNED_TO_BUG": return <PersonAddIcon {...iconSettings} />
        case "UNASSIGNED_FROM_BUG": return <PersonRemoveIcon {...iconSettings} />
        case "BUG_UPDATE": return <EditIcon {...iconSettings} />
        case "NEW_DUPLICATE_REPORTS": return <FileCopyIcon {...iconSettings} />
        case "NEW_DUPLICATE_SUGGESTIONS": return <AwesomeIcon {...iconSettings} />
        case "REPORT_DISCOVERED_AS_DUPLICATE": return <LightIcon {...iconSettings} />
        default: return <SpeakerIcon/>
    }
}