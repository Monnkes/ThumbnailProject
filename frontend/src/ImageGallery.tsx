import React, {Dispatch, SetStateAction, useState} from 'react';
import SelectedImage from './SelectedImage';
import './styles/ImageGallery.css';
import configuration from './frontendConfiguration.json';
import {ThumbnailSize, ThumbnailType} from "./utils/ThumbnailProperties";

interface ImageGalleryProps {
    images: ImageData[];
    originalImage: ImageData;
    socket: WebSocket | null;
    setOriginalImage: Dispatch<SetStateAction<ImageData>>;
    thumbnailTypeRef: React.MutableRefObject<ThumbnailType>;
}

interface ImageData {
    data: string;
    id: number;
}

const ImageGallery: React.FC<ImageGalleryProps> = ({
                                                       images,
                                                       originalImage,
                                                       socket,
                                                       setOriginalImage,
                                                       thumbnailTypeRef
                                                   }) => {
    const [showPopup, setShowPopup] = useState(false);
    const [selectedImage, setSelectedImage] = useState<ImageData | null>(null);

    const handleImageClick = (image: string, index: number) => {
        setSelectedImage({ data: image, id: index });
        setShowPopup(true);
    };

    const handleClosePopup = () => {
        setOriginalImage({ data: configuration.loadingIcon, id: 0 });
        setShowPopup(false);
    };

    const getGridTemplateColumns = () => {
        switch (thumbnailTypeRef.current) {
            case ThumbnailType.SMALL:
                return `repeat(auto-fill, minmax(${ThumbnailSize[ThumbnailType.SMALL].height}px, 1fr))`;
            case ThumbnailType.MEDIUM:
                return `repeat(auto-fill, minmax(${ThumbnailSize[ThumbnailType.MEDIUM].height}px, 1fr))`;
            case ThumbnailType.BIG:
                return `repeat(auto-fill, minmax(${ThumbnailSize[ThumbnailType.BIG].height}px, 1fr))`;
            default:
                return `repeat(auto-fill, minmax(${ThumbnailSize[ThumbnailType.SMALL].height}px, 1fr))`;
        }
    };

    return (
        <div className="gallery" style={{ gridTemplateColumns: getGridTemplateColumns() }}>
            {images.map((base64Image) => (
                <div
                    key={base64Image.id}
                    className="image-container"
                    style={{
                        width: ThumbnailSize[thumbnailTypeRef.current].width,
                        height: ThumbnailSize[thumbnailTypeRef.current].height,
                    }}
                >
                    <img
                        src={`data:image/png;base64,${base64Image.data}`}
                        alt={`Uploaded ${base64Image.id}`}
                        className="image"
                        onClick={() => handleImageClick(base64Image.data, base64Image.id)}
                    />
                </div>
            ))}

            {showPopup && selectedImage && (
                <SelectedImage
                    imageData={selectedImage.data}
                    id={selectedImage.id}
                    originalImage={originalImage}
                    socket={socket}
                    onClose={handleClosePopup}
                />
            )}
        </div>
    );
};

export default ImageGallery;
