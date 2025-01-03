import React, {useState} from 'react';
import './styles/ImageUploader.css';
import texts from './texts/texts.json';
import frontendConfiguration from './frontendConfiguration.json';
import MessageTypes from './utils/MessageTypes';

interface ImageUploaderProps {
    onClose: () => void;
    socket: WebSocket | null;
}

interface ImageData {
    data: string;
    id: number;
}

const calculateBase64Size = (base64: string): number => {
    const padding = (base64.match(/=+$/) || [""])[0].length;
    return (base64.length * 3) / 4 - padding;
};


const ImageUploader: React.FC<ImageUploaderProps> = ({onClose, socket}) => {
    const [selectedFiles, setSelectedFiles] = useState<File[]>([]);

    const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files) {
            setSelectedFiles(Array.from(event.target.files));
        }
    };

    const handleUpload = () => {
        const base64Images: ImageData[] = [];

        const promises = selectedFiles.map((file) => {
            return new Promise<ImageData>((resolve, reject) => {
                const reader = new FileReader();
                reader.onload = () => {
                    const base64Data = reader.result!.toString().split(',')[1];
                    resolve({ data: base64Data, id: 0 });
                };
                reader.onerror = (error) => reject(error);
                reader.readAsDataURL(file);
            });
        });

        Promise.all(promises)
            .then((results) => {
                base64Images.push(...results);

                if (socket && socket.readyState === WebSocket.OPEN) {
                    let currentBatch: ImageData[] = [];
                    let currentSize = 0;
                    let batchCount = 0;

                    const sendImagesBatch = (batch: ImageData[]) => {
                        const message = {
                            type: MessageTypes.UPLOAD_IMAGES,
                            imagesData: batch.map(image => ({data: image.data, id: image.id})),
                        };
                        console.log("SIZE: " + calculateBase64Size(JSON.stringify(message)));
                        socket.send(JSON.stringify(message));
                    };

                   for (const image of base64Images) {
                        const imageSize = calculateBase64Size(image.data);

                        if(imageSize > frontendConfiguration.max_batch_size){
                            alert(`${texts.tooLargeImage}`);
                            continue;
                        }
                        else if (currentSize + imageSize > frontendConfiguration.max_batch_size) {
                            sendImagesBatch(currentBatch);
                            currentSize = 0;
                            currentBatch = [];
                            batchCount++;
                        }
                        if (batchCount < frontendConfiguration.max_batch_amount) {
                            currentBatch.push(image);
                            currentSize += imageSize;
                        }
                        else {
                            alert(`${texts.maxBatchesLimit}`);
                            break;
                        }
                   }
                    if (currentBatch.length > 0 && batchCount < frontendConfiguration.max_batch_amount) {
                        sendImagesBatch(currentBatch);
                    }
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
