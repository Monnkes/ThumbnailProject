import React, {useState, useEffect, useRef} from 'react';
import ImageGallery from './ImageGallery';
import ImageUploader from './ImageUploader';
import texts from './texts/texts.json';
import './styles/App.css';
import configuration from './frontendConfiguration.json';
import MessageTypes from './MessageTypes';
import ResponseStatusTypes from "./ResponseStatusTypes";

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
    const loadingIcon: ImageData = {data: configuration.loadingIcon, id: 0};

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

            if (data.type === MessageTypes.GET_THUMBNAILS_RESPONSE && data.imagesData) {
                addThumbnails(
                    data.imagesData.map((thumbnail: ImageData) => ({
                        id: thumbnail.id,
                        data: thumbnail.data,
                    }))
                );
            }

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

            if (data.type === MessageTypes.INFO_RESPONSE && data.responseStatus === ResponseStatusTypes.UNSUPPORTED_MEDIA_TYPE) {
                setImages((prevImages) => {
                    alert(`${texts.uploadFailure}`);

                    return prevImages[prevImages.length - 1].id === 0
                        ? prevImages.slice(0, prevImages.length - 1)
                        : prevImages;
                });
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
                ws.close();
            }
        };
    }, []);

    const addIcons = (newImages: ImageData[]) => {
        const loadingIcons = new Array(newImages.length).fill(loadingIcon);
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
                <button className="button" onClick={() => setIsUploaderOpen(true)}>
                    {texts.addPhotos}
                </button>
            </header>
            {isUploaderOpen && (
                <ImageUploader
                    onClose={() => setIsUploaderOpen(false)}
                    onUpload={addIcons}
                    socket={socket}
                />
            )}
            <ImageGallery images={images} originalImage={originalImage} socket={socket}
                          setOriginalImage={setOriginalImage}/>
        </div>
    );
}

export default App;
