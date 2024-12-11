import React, { useState, useEffect, useRef } from 'react';
import ImageGallery from './ImageGallery';
import ImageUploader from './ImageUploader';
import texts from './texts/texts.json';
import './styles/App.css';
import configuration from './frontendConfiguration.json';
import MessageTypes from './MessageTypes';

function App() {
    const [images, setImages] = useState<string[]>([]);
    const [numberOfImages, setNumberOfImages] = useState<number>(0);
    const [isUploaderOpen, setIsUploaderOpen] = useState<boolean>(false);
    const [socket, setSocket] = useState<WebSocket | null>(null);
    const loadingIcon: string = configuration.loadingIcon;

    const numberOfThumbnailsRef = useRef<number>(0);
    const imagesRef = useRef<string[]>([]);

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

            if (data.type === MessageTypes.GetImagesResponse && data.imagesData) {
                addThumbnails(data.imagesData);
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

    const addIcons = (newImages: string[]) => {
        const loadingIcons = new Array(newImages.length).fill(loadingIcon);
        setNumberOfImages((prevNumber) => prevNumber + newImages.length);
        setImages((prevImages) => [...prevImages, ...loadingIcons]);
        console.log("Current images (after adding icons):", imagesRef.current);
    };

    const addThumbnails = (thumbnails: string[]) => {
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
            <ImageGallery images={images} socket={socket} />
        </div>
    );
}

export default App;
