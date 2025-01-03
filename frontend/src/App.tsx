import React, { useState, useEffect, useRef } from 'react';
import ImageGallery from './ImageGallery';
import ImageUploader from './ImageUploader';
import texts from './texts/texts.json';
import './styles/App.css';
import configuration from './frontendConfiguration.json';
import MessageTypes from './MessageTypes';
import ResponseStatusTypes from './ResponseStatusTypes';

interface ImageData {
    data: string;
    id: number;
}

const createDefaultImageData = (): ImageData => ({
    data: configuration.loadingIcon,
    id: 0,
});

function App() {
    const [images, setImages] = useState<ImageData[]>([]);
    const [originalImage, setOriginalImage] = useState<ImageData>(createDefaultImageData);
    const [isUploaderOpen, setIsUploaderOpen] = useState<boolean>(false);
    const [socket, setSocket] = useState<WebSocket | null>(null);
    const [thumbnailSize, setThumbnailSize] = useState<'SMALL' | 'MEDIUM' | 'BIG'>('SMALL');

    const numberOfThumbnailsRef = useRef<number>(0);
    const imagesRef = useRef<ImageData[]>([]);

    useEffect(() => {
        imagesRef.current = images;
    }, [images]);

    useEffect(() => {
        const ws = new WebSocket('ws://localhost:8080/upload-files');

        ws.onopen = () => {
            console.log('Connection established');
        };

        ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log('Message received:', data);

            if (data.type === MessageTypes.GET_IMAGE_RESPONSE && data.imagesData && data.imagesData[0]) {
                console.log('Received original image data:', data.imagesData[0]);
                setOriginalImage(data.imagesData[0]);
            }

            if (data.type === MessageTypes.PING) {
                const message = {
                    type: MessageTypes.PONG,
                };
                ws.send(JSON.stringify(message));
                console.log("Message send: ", message);
            }

            if (data.type === MessageTypes.INFO_RESPONSE && data.responseStatus === ResponseStatusTypes.UNSUPPORTED_MEDIA_TYPE) {
                console.log('Unsupported media type received');
                setImages((prevImages) => {
                    alert(`${texts.uploadFailure}`);
                    return prevImages[prevImages.length - 1].id === 0
                        ? prevImages.slice(0, prevImages.length - 1)
                        : prevImages;
                });
            }

            if (data.type === MessageTypes.GET_THUMBNAILS_RESPONSE && data.imagesData) {
                addThumbnails(
                    data.imagesData.map((thumbnail: ImageData) => ({
                        id: thumbnail.id,
                        data: thumbnail.data,
                    }))
                );
            }
        };

        ws.onclose = () => {
            console.log('Connection closed');
        };

        ws.onerror = (error) => {
            console.error('Connection error:', error);
        };

        setSocket(ws);

        return () => {
            if (ws) {
                console.log('Closing WebSocket connection');
                ws.close();
            }
        };
    }, []);

    const fetchThumbnails = (type: string) => {
        console.log('Clearing previous images');
        setImages([]);
        numberOfThumbnailsRef.current = 0;
        console.log(`Fetching ${type} thumbnails...`);
        if (socket && socket.readyState === WebSocket.OPEN) {
            const message = {
                type: `GET_ALL_${type}_THUMBNAILS`,
            };
            console.log('Sending message to server:', message);
            socket.send(JSON.stringify(message));
        }
    };

    const addIcons = (newImages: ImageData[]) => {
        console.log('Adding loading icons...');
        const loadingIcons = new Array(newImages.length).fill(createDefaultImageData());
        setImages((prevImages) => [...prevImages, ...loadingIcons]);
    };

    const addThumbnails = (thumbnails: ImageData[]) => {
        console.log('Adding new thumbnails...');
        setImages(prevImages => {
            const updatedImages = [...prevImages];
            for (let i = numberOfThumbnailsRef.current; i < numberOfThumbnailsRef.current + thumbnails.length; i++) {
                updatedImages[i] = thumbnails[i - numberOfThumbnailsRef.current];
            }
            numberOfThumbnailsRef.current += thumbnails.length;
            console.log('Updated images:', updatedImages);
            return updatedImages;
        });
    };

    return (
        <div>
            <header className="header">
                <h1 className="title">{texts.title}</h1>
                <div className="button-group">
                    <button className="button" onClick={() => setIsUploaderOpen(true)}>
                        {texts.addPhotos}
                    </button>
                    <div className="thumbnail-buttons">
                        <button
                            className={`button ${thumbnailSize === 'SMALL' ? 'active' : ''}`}
                            onClick={() => {
                                setThumbnailSize('SMALL');
                                fetchThumbnails('SMALL');
                            }}
                        >Small
                        </button>
                        <button
                            className={`button ${thumbnailSize === 'MEDIUM' ? 'active' : ''}`}
                            onClick={() => {
                                setThumbnailSize('MEDIUM');
                                fetchThumbnails('MEDIUM');
                            }}
                        >Medium
                        </button>
                        <button
                            className={`button ${thumbnailSize === 'BIG' ? 'active' : ''}`}
                            onClick={() => {
                                setThumbnailSize('BIG');
                                fetchThumbnails('BIG');
                            }}
                        >Big
                        </button>
                    </div>
                </div>
            </header>

            {isUploaderOpen && (
                <ImageUploader
                    onClose={() => setIsUploaderOpen(false)}
                    onUpload={addIcons}
                    socket={socket}
                />
            )}
            <ImageGallery images={images} originalImage={originalImage} socket={socket} setOriginalImage={setOriginalImage} />
        </div>
    );
}

export default App;
