import React, {Dispatch, SetStateAction, useEffect} from 'react';
import './styles/SelectedImage.css';
import MessageTypes from "./MessageTypes";

interface SelectedImageProps {
    imageData: string;
    originalImage: ImageData
    id: number;
    socket: WebSocket | null;
    onClose: () => void;
}

interface ImageData {
    data: string;
    id: number;
}

const SelectedImage: React.FC<SelectedImageProps> = ({ imageData, id, originalImage, socket, onClose }) => {
    useEffect(() => {
        handleUpload();
    }, []);

    const handleUpload = () => {
        const promises = [new Promise<string>((resolve) => resolve(imageData))];

        Promise.all(promises)
            .then(() => {

                if (socket && socket.readyState === WebSocket.OPEN) {
                    const message = {
                        type: MessageTypes.GET_IMAGE,
                        ids: [id],
                    };
                    socket.send(JSON.stringify(message));
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
                <img src={`data:image/png;base64,${originalImage.data}`} alt={`Selected ${originalImage.id}`} className="popup-image"/>
            </div>
        </div>
    );
};

export default SelectedImage;
