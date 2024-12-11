import React, { useState } from 'react';
import SelectedImage from './SelectedImage';
import './styles/ImageGallery.css';

interface ImageGalleryProps {
    images: string[];
    socket: WebSocket | null;
}

const ImageGallery: React.FC<ImageGalleryProps> = ({ images, socket }) => {
    const [selectedImageData, setSelectedImageData] = useState<string | null>(null);
    const [selectedImageId, setSelectedImageId] = useState<number | null>(null);
    const [showPopup, setShowPopup] = useState(false);

    const handleImageClick = (image: string, index: number) => {
        setSelectedImageData(image);
        setSelectedImageId(index);
        setShowPopup(true);
    };

    const handleClosePopup = () => {
        setShowPopup(false);
    };

    const handleUpload = (images: string[]) => {
        console.log('Uploaded images: ', images);
    };

    return (
        <div className="gallery">
            {images.map((base64Image) => (
                <div key={0} className="image-container">
                    <img
                        src={`data:image/png;base64,${base64Image}`}
                        alt={`Uploaded ${0}`}
                        className="image"
                        onClick={() => handleImageClick(base64Image, 0)}
                    />
                </div>
            ))}

            {showPopup && selectedImageData && selectedImageId !== null && (
                <SelectedImage
                    imageData={selectedImageData}
                    id={selectedImageId}
                    socket={socket}
                    onUpload={handleUpload}
                    onClose={handleClosePopup}
                />
            )}
        </div>
    );
};

export default ImageGallery;
