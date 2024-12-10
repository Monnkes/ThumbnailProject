import React, { useState } from 'react';
import './styles/ImageUploader.css';

const ImageUploader = ({ onClose, onUpload, socket }) => {
    const [selectedFiles, setSelectedFiles] = useState([]);

    const handleFileChange = (event) => {
        setSelectedFiles(Array.from(event.target.files));
    };

    const handleUpload = () => {
        const base64Images = [];

        // Konwersja plików na Base64
        const promises = selectedFiles.map((file) => {
            return new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(reader.result.split(',')[1]); // Pobieramy tylko dane Base64
                reader.onerror = (error) => reject(error);
                reader.readAsDataURL(file);
            });
        });

        Promise.all(promises)
            .then((results) => {
                base64Images.push(...results);
                onUpload(base64Images); // Aktualizacja stanu w głównym komponencie

                // Wysłanie informacji do serwera przez WebSocket
                if (socket && socket.readyState === WebSocket.OPEN) {
                    console.log("dupa")
                    const message = {
                        type: 'UploadImages',
                        imagesData: base64Images,
                    };
                    socket.send(JSON.stringify(message));
                    console.log(JSON.stringify(message));
                }

                setSelectedFiles([]); // Czyszczenie wyboru plików
                onClose(); // Zamknięcie modalnego okna
            })
            .catch((error) => console.error('Błąd podczas odczytu plików:', error));
    };

    return (
        <div className="overlay">
            <div className="panel">
                <h2>Upload Images</h2>
                <input type="file" multiple accept="image/*" onChange={handleFileChange} />
                <div className="buttons">
                    <button className="button" onClick={handleUpload}>
                        Upload
                    </button>
                    <button className="button" onClick={onClose}>
                        Cancel
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ImageUploader;
