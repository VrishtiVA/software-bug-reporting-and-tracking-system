import Slider from "react-slick";
import "slick-carousel/slick/slick.css"; 
import "slick-carousel/slick/slick-theme.css";
import { Box } from "@mui/material";
import { useMemo, useState } from "react";

export default function SlickCarousel({children}) {

    const [currentIndex, setCurrentIndex] = useState(0);

    const sliderSettings = useMemo(() => ({
        dots: true,
        arrows: false,
        infinite: true,
        slidesToShow: 1,
        slidesToScroll: 1,
        swipe: true,
        adaptiveHeight: true,
        beforeChange: (currentI, newI) => {
            setCurrentIndex(newI);
        },
        //Custom pagination bullets
        customPaging: i => (
            <Box sx={{
                width: 9,
                height: 9,
                transform: i === currentIndex ? "scale(1.2)" : "scale(1)",
                borderRadius: "50%",
                backgroundColor: i === currentIndex ? "primary.main" : "divider",
                display: "inline-block",
                transition: "transform 0.3s ease-in-out, background-color 0.3s ease-in-out"
            }}/>
        )
    }), [currentIndex]);

    return (<>
        <Box mb={2} sx={{cursor: "grab", "&:active": {cursor: "grabbing"}}}>
            <Slider {...sliderSettings}>
                {children}
            </Slider>
        </Box>
    </>);
}