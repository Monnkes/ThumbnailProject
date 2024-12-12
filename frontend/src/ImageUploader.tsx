import React, {useState} from 'react';
import './styles/ImageUploader.css';
import texts from './texts/texts.json';
import MessageTypes from './MessageTypes';

interface ImageUploaderProps {
    onClose: () => void;
    onUpload: (base64Images: string[]) => void;
    socket: WebSocket | null;
}

const ImageUploader: React.FC<ImageUploaderProps> = ({onClose, onUpload, socket}) => {
    const [selectedFiles, setSelectedFiles] = useState<File[]>([]);

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) {
            setSelectedFiles(Array.from(event.target.files));
        }
    };

    const handleUpload = () => {
        const base64Images: string[] = [];

        const promises = selectedFiles.map((file) => {
            return new Promise<string>((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => resolve(reader.result!.toString().split(',')[1]);
                reader.onerror = (error) => reject(error);
                reader.readAsDataURL(file);
            });
        });

        Promise.all(promises)
            .then((results) => {
                base64Images.push(...results);
                onUpload(base64Images);

                if (socket && socket.readyState === WebSocket.OPEN) {
                    const message = {
                        type: "UploadImages",
                        imagesData: base64Images.map(image => ({ data: image, id: null })),
                    };
                    console.log(message);
                    socket.send(JSON.stringify(message));

                    // // ONLY FOR TESTING REMOVE
                    // for (let i = 0; i < base64Images.length; i++) {
                    //     const getMessage = {
                    //         type: MessageTypes.GetImages,
                    //         ids: [i],
                    //     };
                    //     socket.send(JSON.stringify(getMessage));
                    // }
                }

                setSelectedFiles([]);
                onClose();
            })
            .catch((error) => console.error('Error reading files: ', error));
    };

    return (
        <div className="overlay">
            <div className="panel">
                <h2>{texts.uploadImages}</h2>
                <input
                    type="file"
                    multiple
                    accept="image/*"
                    onChange={handleFileChange}
                />
                <div className="buttons">
                    <button className="button" onClick={handleUpload}>
                        {texts.upload}
                    </button>
                    <button className="button" onClick={onClose}>
                        {texts.cancel}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ImageUploader;
