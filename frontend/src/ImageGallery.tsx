import React, {Dispatch, SetStateAction, useState} from 'react';
import SelectedImage from './SelectedImage';
import './styles/ImageGallery.css';
import configuration from './frontendConfiguration.json'

interface ImageGalleryProps {
    images: ImageData[];
    originalImage: ImageData;
    socket: WebSocket | null;
    setOriginalImage: Dispatch<SetStateAction<ImageData>>;
}

interface ImageData {
    data: string;
    id: number;
}

const ImageGallery: React.FC<ImageGalleryProps> = ({ images, originalImage, socket, setOriginalImage }) => {
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

    return (
        <div className="gallery">
            {images.map((base64Image) => (
                <div key={0} className="image-container">
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
