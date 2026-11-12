import { Fancybox } from "@fancyapps/ui";

/**
 * Fancybox creation, with cleanup handled on its destruction. 
 * Fancybox can therefore be displayed using just this function.
*/
export function showFancybox(resourceURL, caption, type, filename, destroyURL=true) {
    Fancybox.show(
        [{
            src: resourceURL,
            caption: caption,
            type: type,
            downloadSrc: resourceURL,
            downloadFilename: filename
        }], {
            Carousel: {
                Toolbar: {
                    display: {
                        left: ["reset", "zoomIn", "zoomOut", "toggle1to1", "rotateCCW", "rotateCW", "fullscreen"],
                        right: ["download", "close"]
                    }
                }
            },
            on: {
                //Cleanup to free memory.
                destroy: () => {
                    if (destroyURL) URL.revokeObjectURL(resourceURL);
                }
            },
        }
    );
}