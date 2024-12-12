import React, { useState } from 'react';
import SelectedImage from './SelectedImage';
import './styles/ImageGallery.css';

interface ImageGalleryProps {
    images: ImageData[];
    socket: WebSocket | null;
}

interface ImageData {
    data: string;
    id: number;
}

const ImageGallery: React.FC<ImageGalleryProps> = ({ images, socket }) => {
    const [selectedImage, setSelectedImage] = useState<ImageData | null>(null);
    const [showPopup, setShowPopup] = useState(false);

    const handleImageClick = (image: string, index: number) => {
        setSelectedImage({ data: image, id: index });
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
                        src={`data:image/png;base64,${base64Image.data}`}
                        alt={`Uploaded ${0}`}
                        className="image"
                        onClick={() => handleImageClick(base64Image.data, base64Image.id)}
                    />
                </div>
            ))}

            {showPopup && selectedImage && (
                <SelectedImage
                    imageData={selectedImage.data}
                    id={selectedImage.id}
                    socket={socket}
                    onUpload={handleUpload}
                    onClose={handleClosePopup}
                />
            )}
        </div>
    );
};

export default ImageGallery;
