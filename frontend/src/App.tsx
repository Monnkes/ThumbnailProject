import React, {useEffect, useRef, useState} from 'react';
import ImageGallery from './ImageGallery';
import ImageUploader from './ImageUploader';
import texts from './texts/texts.json';
import './styles/App.css';
import configuration from './frontendConfiguration.json';
import MessageTypes from './utils/MessageTypes';
import ResponseStatusTypes from './utils/ResponseStatusTypes';
import {ThumbnailType} from "./utils/ThumbnailProperties";

interface ImageData {
    data: string;
    id: number;
    iconOrder: number;
}

const createDefaultImageData = (): ImageData => ({
    data: configuration.loadingIcon,
    id: 0,
    iconOrder: -1,
});

function App() {
    const [images, setImages] = useState<ImageData[]>([]);
    const [originalImage, setOriginalImage] = useState<ImageData>(createDefaultImageData);
    const [isUploaderOpen, setIsUploaderOpen] = useState<boolean>(false);
    const [socket, setSocket] = useState<WebSocket | null>(null);
    const thumbnailTypeRef = useRef<ThumbnailType>(ThumbnailType.SMALL);
    const numberOfThumbnailsRef = useRef<number>(0);
    const imagesRef = useRef<ImageData[]>([]);
    const [connectionStatus, setConnectionStatus] = useState<string>('connecting');
    const [thumbnailsMagazine, setThumbnailsMagazine] = useState<ImageData[]>([])


    useEffect(() => {
        imagesRef.current = images;
    }, [images]);

    useEffect(() => {
        let reconnectInterval: NodeJS.Timeout | null = null;

        const connectWebSocket = () => {
            const ws = new WebSocket('ws://localhost:8080/upload-files');

            ws.onopen = () => {
                console.log('Connection established');
                setSocket(ws);
                setConnectionStatus('connected');
                setImages([]);

                if (reconnectInterval) {
                    clearInterval(reconnectInterval);
                    reconnectInterval = null;
                }

                const message = {
                    type: `GET_THUMBNAILS`,
                    thumbnailType: thumbnailTypeRef.current,
                };
                console.log('Sending message to server on reconnect:', message);
                ws.send(JSON.stringify(message));
            };

            ws.onmessage = (event) => {
                const data = JSON.parse(event.data);
                console.log('Message received:', data);

                if (data.messageType === MessageTypes.GET_IMAGE && data.imagesData && data.imagesData[0]) {
                    setOriginalImage(data.imagesData[0]);
                }

                if (data.messageType === MessageTypes.PING) {
                    const message = {
                        type: MessageTypes.PONG,
                    };
                    ws.send(JSON.stringify(message));
                    console.log('Message sent:', message);
                }

                if (data.messageType === MessageTypes.INFO_RESPONSE) {
                    if (data.responseStatus === ResponseStatusTypes.UNSUPPORTED_MEDIA_TYPE) {
                        console.log('Unsupported media type received');
                        setImages((prevImages) => {
                            alert(`${texts.uploadFailure}`);
                            return prevImages[prevImages.length - 1].id === 0
                                ? prevImages.slice(0, prevImages.length - 1)
                                : prevImages;
                        });
                    }
            }

            if (data.messageType === MessageTypes.PLACEHOLDERS_NUMBER_RESPONSE) {
                if (data.thumbnailsNumber !== null && data.thumbnailsNumber > 0) {
                    addIcons(data.thumbnailsNumber);
                    addThumbnails(thumbnailsMagazine);
                }
            }

            if (data.messageType === MessageTypes.GET_THUMBNAILS && data.imagesData) {
                if (data.thumbnailType === thumbnailTypeRef.current) {
                    if (imagesRef.current.length === 0) {
                        setThumbnailsMagazine((prevMagazine) => [
                            ...prevMagazine,
                            ...data.imagesData.map((thumbnail: ImageData) => ({
                                id: thumbnail.id,
                                data: thumbnail.data,
                                iconOrder: thumbnail.iconOrder
                            }))
                        ]);
                        console.log('Thumbnails added to magazine: ', data.imagesData);
                    } else {
                        addThumbnails(
                            data.imagesData.map((thumbnail: ImageData) => ({
                                id: thumbnail.id,
                                data: thumbnail.data,
                                iconOrder: thumbnail.iconOrder
                            }))
                        );
                    }
                }
            }
        };

            ws.onclose = () => {
                console.log('Connection closed. Attempting to reconnect...');
                setConnectionStatus('connecting');
                if (!reconnectInterval) {
                    reconnectInterval = setInterval(() => {
                        connectWebSocket();
                    }, configuration.reconnecting_interval);
                }
            };

            ws.onerror = (error) => {
                console.error('Connection error:', error);
                setConnectionStatus('disconnected');
                ws.close();
            };
        };

        connectWebSocket();

        return () => {
            if (reconnectInterval) {
                clearInterval(reconnectInterval);
            }
            if (socket) {
                console.log('Closing WebSocket connection');
                socket.close();
            }
        };
    }, []);

    const fetchThumbnails = (type: ThumbnailType) => {
        setImages([]);
        numberOfThumbnailsRef.current = 0;
        console.log(`New thumbnails type: ${thumbnailTypeRef.current}`)
        if (socket && socket.readyState === WebSocket.OPEN) {
            const message = {
                type: `GET_THUMBNAILS`,
                thumbnailType: thumbnailTypeRef.current,
            };
            console.log('Sending message to server:', message);
            socket.send(JSON.stringify(message));
        }
    };

    const addIcons = (thumbnailsNumber: number) => {
        setImages((prevImages) => {
            const placeholderCount = prevImages.filter((image) => image.id === 0).length;

            if (placeholderCount < thumbnailsNumber) {
                const newPlaceholders = new Array(thumbnailsNumber - placeholderCount).fill(createDefaultImageData());
                return [...prevImages, ...newPlaceholders];
            }

            return prevImages;
        });
    };

    const addThumbnails = (thumbnails: ImageData[]) => {
        setImages(prevImages => {
            const updatedImages = [...prevImages];

            for (const thumbnail of thumbnails) {
                if(thumbnail.data === undefined || thumbnail.data === null) {
                    console.error("Thumbnail with missing data: " + thumbnail);
                    continue;
                }
                updatedImages[thumbnail.iconOrder] = thumbnail;
            }

            return updatedImages;
        });
    };

    // TODO Add sending placeholders during changing ThumbnailType
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
                            className={`button ${thumbnailTypeRef.current === ThumbnailType.SMALL ? 'active' : ''}`}
                            onClick={() => {
                                thumbnailTypeRef.current = ThumbnailType.SMALL;
                                fetchThumbnails(ThumbnailType.SMALL);
                            }}
                        >{texts.small}
                        </button>
                        <button
                            className={`button ${thumbnailTypeRef.current === ThumbnailType.MEDIUM ? 'active' : ''}`}
                            onClick={() => {
                                thumbnailTypeRef.current = ThumbnailType.MEDIUM;
                                fetchThumbnails(ThumbnailType.MEDIUM);
                            }}
                        >{texts.medium}
                        </button>
                        <button
                            className={`button ${thumbnailTypeRef.current === ThumbnailType.BIG ? 'active' : ''}`}
                            onClick={() => {
                                thumbnailTypeRef.current = ThumbnailType.BIG;
                                fetchThumbnails(ThumbnailType.BIG);
                            }}
                        >{texts.big}
                        </button>
                    </div>
                </div>
            </header>

            {connectionStatus === 'connecting' && (
                <div className="connection-status">
                    <p>Connecting...</p>
                </div>
            )}
            {connectionStatus === 'disconnected' && (
                <div className="connection-status">
                    <p>Connection lost. Retrying...</p>
                </div>
            )}

            {isUploaderOpen && (
                <ImageUploader
                    onClose={() => setIsUploaderOpen(false)}
                    socket={socket}
                />
            )}
            <ImageGallery
                images={images}
                originalImage={originalImage}
                socket={socket}
                setOriginalImage={setOriginalImage}
                thumbnailTypeRef={thumbnailTypeRef}
            />
        </div>
    );
}

export default App;
