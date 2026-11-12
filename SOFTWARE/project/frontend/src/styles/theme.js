import { alpha, createTheme } from "@mui/material/styles"
import { cyan } from '@mui/material/colors';
import { WorkspaceColours } from "../api/colours";

/* 
    The reference used to learn how to define a global theme: 
    https://dev.to/victoria_limadecarvalho/mui-material-styling-how-to-implement-it-in-your-react-app-3i40
*/

const theme = createTheme({
    palette: {
        primary: {
            main: cyan[700],
            surface: cyan[50]
        },
        error: {
            main: "#d72b2b"
        },
        success: {
            main: "#3a9e3f"
        },
        old: {
            main: "#f8dede"
        },
        new: {
            main: "#d8edce"
        },
        neutral: {
            main: "#fafafa"
        },
        workspaceColours: WorkspaceColours
    },
    components: {
        MuiTooltip: {
            defaultProps: {
                arrow: true,
                enterTouchDelay: 0,
                placement: "top",
                style: {
                    cursor: "help"
                },
                slotProps: {
                    popper: {
                        modifiers: [
                            {
                                name: "offset",
                                options: {
                                    offset: [0, -8]
                                }
                            }
                        ]
                    }
                }
            },
            styleOverrides: {
                tooltip: {
                    lineHeight: 1.4,
                    maxWidth: 220,
                    letterSpacing: 0.2, //Increase easy of reading
                    backgroundColor: "#242424e0"
                },
                arrow: {
                    color: "#242424e0"
                }
            }
        },
        MuiTextField: {
            defaultProps: {
                size: "small",
                margin: "dense",
            },
        },
        MuiOutlinedInput: {
            defaultProps: {
                size: "small"
            }
        },
        MuiToggleButton: {
            styleOverrides: {
                root: {
                    display: "flex",
                    alignItems: "center",
                    gap: "4px",
                    paddingLeft: "10px",
                    paddingRight: "10px",
                    textTransform: "none",
                    fontSize: "14px"
                }
            }
        },
        MuiFormLabel: {
            styleOverrides: {
                asterisk: {
                    color: "#e40000",
                    fontWeight: "bold"
                }
            }
        },
        MuiFormControlLabel: {
            variants: [
                {
                    props: { variant: "hoverable" },
                    style: ({theme}) => ({
                        margin: 0,
                        padding: "4px 15px 4px 4px",
                        transition: "background-color 0.3s ease-in-out",
                        "&:hover": {
                            backgroundColor: theme.palette.action.hover
                        },
                        "&:has(.MuiRadio-root.Mui-checked)": {
                            backgroundColor: alpha(theme.palette.primary.main, theme.palette.action.hoverOpacity),
                        },
                        "&:has(.MuiRadio-root.Mui-checked):hover": {
                            backgroundColor: alpha(theme.palette.primary.main, theme.palette.action.selectedOpacity),
                        },
                        "&.Mui-focusVisible": {
                            backgroundColor: theme.palette.action.focus,
                        },
                        "&:not(:last-child)": {
                            borderBottom: `1px solid ${theme.palette.divider}`
                        }
                    })
                }
            ]
        },
        MuiToggleButtonGroup: {
            defaultProps: {
                size: "small"
            }
        },
        MuiSelect: {
            defaultProps: {
                size: "small",
                MenuProps:{
                    PaperProps: {
                        sx: {
                            maxHeight: 200
                        }
                    }
                }
            },
            styleOverrides: {
                select: {
                    display: "flex",
                    gap: "7px",
                    alignItems: "center"
                },   
            }
        },
        MuiButtonGroup: {
            defaultProps: {
                disableElevation: true
            },
        },
        MuiButton: {
            defaultProps: {
                disableElevation: true
            },
            styleOverrides: {
                root: {
                    textTransform: "none"
                },
                sizeMedium: {
                    fontSize: "14px"
                }
            }
        },
        MuiDrawer: {
            styleOverrides: {
                paper: {
                    backgroundColor: "#fafafa"
                }
            }
        },
        MuiMenu: {
            defaultProps: {
                elevation: 3
            },
            styleOverrides: {
                paper: {
                    backgroundColor: "#ffffff",
                }
            }
        },
        MuiTab: {
            defaultProps: {
                disableRipple: true,
            },
            styleOverrides: {
                root: {
                    textTransform: "none",
                    padding: "0 10px"
                }
            }
        }, 
        MuiChip: {
            variants: [
                {
                    props: { variant: "soft-square" },
                    style: {
                        borderRadius: "5px"
                    }
                },
                {
                    props: { variant: "outlined-soft-square" },
                    style: ({theme}) => ({
                        borderRadius: "5px",
                        border: "1px solid " + theme.palette.divider,
                        backgroundColor: "white"
                    })
                },
                {
                    props: { strength: "weak" },
                    style: {
                        backgroundColor: "rgba(0,0,0,0.04)",
                    }
                },
                {
                    props: { variant: "rating" },
                    style: {
                        borderRadius: "5px"
                    }
                }, 
                {
                    props: { variant: "role" },
                    style: {
                        height: "16px", 
                        fontSize: "11px",
                        borderRadius: "4px", 
                        backgroundColor: "rgba(0,0,0,0.04)",
                        ".MuiChip-label": {
                            padding: "4px"
                        }
                    }
                },
                {
                    props: { variant: "id" },
                    style: {
                        height: "18px", 
                        fontSize: "12px",
                        ".MuiChip-label": {
                            padding: "4px 5.5px"
                        }
                    }
                }
            ]
        },
        MuiAccordion: {
            defaultProps: {
                variant: "outlined",
            }, 
            styleOverrides: {
                root: {
                    border: "none",
                    overflow: "hidden",
                    border: "1px solid rgba(0,0,0,0.175)",
                    //First accordian
                    "&:first-of-type": {
                        borderTopLeftRadius: "8px !important",
                        borderTopRightRadius: "8px !important",
                        borderBottom: "none"
                    },
                    //Last accordian
                    "&:last-of-type": {
                        borderBottomLeftRadius: "8px !important",
                        borderBottomRightRadius: "8px !important",
                    },
                    //Middle accordian
                    "&:not(:first-of-type):not(:last-of-type)": {
                        borderRadius: "0",
                        borderBottom: "none"
                    },
                    //Accordian is open
                    "&.Mui-expanded": {
                        borderBottom: "1px solid rgba(0,0,0,0.175) !important",
                        borderRadius: "8px !important"
                    },
                    //The accordian before the open one
                    "&:has(+ .Mui-expanded)": {
                        borderBottomLeftRadius: "8px !important",
                        borderBottomRightRadius: "8px !important",
                        borderBottom: "1px solid rgba(0,0,0,0.175) !important",
                    },
                    //The accordian after the open one
                    "&.Mui-expanded + .MuiAccordion-root": {
                        borderTopLeftRadius: "8px !important",
                        borderTopRightRadius: "8px !important",
                    }
                }
            }
        },
        MuiAccordionSummary: {
            styleOverrides: {
                root: {
                    backgroundColor: "#fafafa",
                    minHeight: "48px",
                    "&.Mui-expanded": {
                        minHeight: "48px"
                    }
                },
                content: {
                    margin: "12px 0",
                    "&.Mui-expanded": {
                        margin: "12px 0"
                    }
                }
            }
        },
        MuiAccordionDetails: {
            styleOverrides: {
                root: {
                    borderTop: "1px solid rgba(0,0,0,0.15)",
                    paddingTop: "15px"
                }
            }
        },
        MuiListItemAvatar: {
            styleOverrides: {
                root: {
                    minWidth: 0,
                    marginRight: 10
                }
            }
        },
        MuiImageListItemBar: {
            styleOverrides: {
                root: {
                    background: "linear-gradient(to top, rgba(0, 0, 0, 0.5) 0%, rgba(0,0,0,0.2) 55%, rgba(0,0,0,0) 100%)",
                },
                titleWrap: {
                    padding: "8px 12px",
                    paddingTop: "10px",
                }
            }
        },
        MuiBadge: {
            styleOverrides: {
                badge: {
                    transform: "translate(55%, -55%)"
                }
            }
        }
    }
});

export default theme;