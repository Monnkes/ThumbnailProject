import React, {useEffect, useState} from 'react';
import './styles/SelectedImage.css';
import MessageTypes from "./MessageTypes";

interface SelectedImageProps {
    imageData: string;
    id: number;
    socket: WebSocket | null;
    onUpload: (images: string[]) => void;
    onClose: () => void;
}

const SelectedImage: React.FC<SelectedImageProps> = ({ imageData, id, socket, onUpload, onClose }) => {
    const [originalImage, setOriginalImage] = useState<string>('');

    useEffect(() => {
        handleUpload();
    }, []);

    const handleUpload = () => {
        const promises = [new Promise<string>((resolve) => resolve(imageData))];

        Promise.all(promises)
            .then((results) => {
                onUpload(results);

                if (socket && socket.readyState === WebSocket.OPEN) {
                    const message = {
                        type: MessageTypes.GetImages,
                        ids: [id],
                    };
                    socket.send(JSON.stringify(message));

                    socket.onmessage = (event) => {
                        const data = JSON.parse(event.data);
                        if (data.type === MessageTypes.GetImagesResponse && data.imagesData && data.imagesData[0]) {
                            setOriginalImage(data.imagesData[0]);
                        }
                    };
                }
            })
            .catch((error) => {
                console.error('Error reading files: ', error);
            });
    };

    return (
        <div className="popup">
            <div className="popup-content">
                <button className="popup-close" onClick={onClose}>X</button>
                <h3></h3>
                <img src={`data:image/png;base64,${originalImage}`} alt={`Selected ${id}`} className="popup-image"/>
            </div>
        </div>
    );
};

export default SelectedImage;
