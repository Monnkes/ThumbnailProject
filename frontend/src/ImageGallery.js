import React from 'react';
import './styles/ImageGallery.css';

const ImageGallery = ({ images }) => {
    if (images.length === 0) {
        return <p className="no-images">No images uploaded yet.</p>;
    }

    return (
        <div className="gallery">
            {images.map((base64Image, index) => (
                <div key={index}>
                    <img
                        src={`data:image/png;base64,${base64Image}`}
                        alt={`Uploaded ${index}`}
                        className="image"
                    />
                </div>
            ))}
        </div>
    );
};

export default ImageGallery;