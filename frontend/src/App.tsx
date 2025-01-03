import React, {useEffect, useRef, useState} from 'react';
import ImageGallery from './ImageGallery';
import ImageUploader from './ImageUploader';
import texts from './texts/texts.json';
import './styles/App.css';
import configuration from './frontendConfiguration.json';
import MessageTypes from './utils/MessageTypes';
import ResponseStatusTypes from './utils/ResponseStatusTypes';
import ThumbnailType from "./utils/ThumbnailType";

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
    const thumbnailSizeRef = useRef<ThumbnailType>(ThumbnailType.SMALL);
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
                setOriginalImage(data.imagesData[0]);
            }

            if (data.type === MessageTypes.PING) {
                const message = {
                    type: MessageTypes.PONG,
                };
                ws.send(JSON.stringify(message));
                console.log("Message send: ", message);
            }

            if (data.type === MessageTypes.INFO_RESPONSE) {
                if(data.responseStatus === ResponseStatusTypes.UNSUPPORTED_MEDIA_TYPE){
                    console.log('Unsupported media type received');
                    setImages((prevImages) => {
                        alert(`${texts.uploadFailure}`);
                        return prevImages[prevImages.length - 1].id === 0
                            ? prevImages.slice(0, prevImages.length - 1)
                            : prevImages;
                    });
                }
                if(data.thumbnailsNumber !== null && data.thumbnailsNumber > 0){
                    addIcons(data.thumbnailsNumber);
                }
            }

            if (data.type === MessageTypes.GET_THUMBNAILS_RESPONSE && data.imagesData) {
                if (data.thumbnailType === thumbnailSizeRef.current) {
                    addThumbnails(
                        data.imagesData.map((thumbnail: ImageData) => ({
                            id: thumbnail.id,
                            data: thumbnail.data,
                        }))
                    );
                }
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

    const fetchThumbnails = (type: ThumbnailType) => {
        setImages([]);
        numberOfThumbnailsRef.current = 0;
        console.log(`New thumbnails type: ${thumbnailSizeRef.current}`)
        if (socket && socket.readyState === WebSocket.OPEN) {
            const message = {
                type: `GET_ALL_${type}_THUMBNAILS`,
                thumbnailType: thumbnailSizeRef.current,
            };
            console.log('Sending message to server:', message);
            socket.send(JSON.stringify(message));
        }
    };

    const addIcons = (thumbnailsNumber: number) => {
        const loadingIcons = new Array(thumbnailsNumber).fill(createDefaultImageData());
        setImages((prevImages) => [...prevImages, ...loadingIcons]);
    };

    const addThumbnails = (thumbnails: ImageData[]) => {
        setImages(prevImages => {
            const updatedImages = [...prevImages];
            for (let i = numberOfThumbnailsRef.current; i < numberOfThumbnailsRef.current + thumbnails.length; i++) {
                updatedImages[i] = thumbnails[i - numberOfThumbnailsRef.current];
            }
            numberOfThumbnailsRef.current += thumbnails.length;
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
                            className={`button ${thumbnailSizeRef.current === ThumbnailType.SMALL ? 'active' : ''}`}
                            onClick={() => {
                                thumbnailSizeRef.current = ThumbnailType.SMALL;
                                fetchThumbnails(ThumbnailType.SMALL);
                            }}
                        >{texts.small}
                        </button>
                        <button
                            className={`button ${thumbnailSizeRef.current === ThumbnailType.MEDIUM ? 'active' : ''}`}
                            onClick={() => {
                                thumbnailSizeRef.current = ThumbnailType.MEDIUM;
                                fetchThumbnails(ThumbnailType.MEDIUM);
                            }}
                        >{texts.medium}
                        </button>
                        <button
                            className={`button ${thumbnailSizeRef.current === ThumbnailType.BIG ? 'active' : ''}`}
                            onClick={() => {
                                thumbnailSizeRef.current = ThumbnailType.BIG;
                                fetchThumbnails(ThumbnailType.BIG);
                            }}
                        >{texts.big}
                        </button>
                    </div>
                </div>
            </header>

            {isUploaderOpen && (
                <ImageUploader
                    onClose={() => setIsUploaderOpen(false)}
                    socket={socket}
                />
            )}
            <ImageGallery images={images} originalImage={originalImage} socket={socket} setOriginalImage={setOriginalImage} />
        </div>
    );
}

export default App;
