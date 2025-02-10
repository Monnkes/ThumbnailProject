import React, {useEffect, useRef, useState} from 'react';
import ImageGallery from './ImageGallery';
import ImageUploader from './ImageUploader';
import Spinner from './Spinner';
import texts from './texts/texts.json';
import './styles/App.css';
import configuration from './frontendConfiguration.json';
import MessageTypes from './utils/MessageTypes';
import ResponseStatusTypes from './utils/ResponseStatusTypes';
import {ThumbnailType} from "./utils/ThumbnailProperties";
import ConnectionTypes from "./utils/ConnectionTypes";
import connectionTypes from "./utils/ConnectionTypes";

interface ImageData {
    data: string;
    id: number;
    iconOrder: number;
    name?: string;
}

interface Folder {
    id: number;
    parentId: number;
    name: string;
}

const createDefaultImageData = (): ImageData => ({
    data: configuration.loadingIcon,
    id: 0,
    iconOrder: -1,
});

function App() {
    const [images, setImages] = useState<ImageData[]>([]);
    const [folders, setFolder] = useState<ImageData[]>([]);
    const [originalImage, setOriginalImage] = useState<ImageData>(createDefaultImageData);
    const [isUploaderOpen, setIsUploaderOpen] = useState<boolean>(false);
    const [socket, setSocket] = useState<WebSocket | null>(null);
    const [selectedImages, setSelectedImages] = useState<number[]>([]);
    const thumbnailTypeRef = useRef<ThumbnailType>(ThumbnailType.SMALL);
    const currentFolderIdRef = useRef<number>(0);
    const parentFolderIdRef = useRef<number>(0);
    const numberOfThumbnailsRef = useRef<number>(0);
    const imagesRef = useRef<ImageData[]>([]);
    const foldersRef = useRef<ImageData[]>([]);
    const [connectionStatus, setConnectionStatus] = useState<string>('connecting');
    const [thumbnailsMagazine, setThumbnailsMagazine] = useState<ImageData[]>([])
    const numberOfCurrentPage = useRef<number>(1);
    const numberOfThumbnailsOnPageRef = useRef<number>(0);
    const bottomRef = useRef<HTMLDivElement | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isObserverEnabled, setIsObserverEnabled] = useState(true);
    const [showMovePopup, setShowMovePopup] = useState(false);


    useEffect(() => {
        imagesRef.current = images;
        if (thumbnailsMagazine.length) {
            addThumbnails(thumbnailsMagazine);
            setThumbnailsMagazine([]);
        }
    }, [images, thumbnailsMagazine, imagesRef]);

    useEffect(() => {
        foldersRef.current = folders;
    }, [folders, foldersRef]);


    useEffect(() => {
        let reconnectInterval: NodeJS.Timeout | null = null;

        const connectWebSocket = () => {
            const ws = new WebSocket('ws://localhost:8080/upload-files');

            ws.onopen = () => {
                console.log('Connection established');
                setSocket(ws);
                setConnectionStatus(ConnectionTypes.CONNECTED);
                setImages([]);

                if (reconnectInterval) {
                    clearInterval(reconnectInterval);
                    reconnectInterval = null;
                }

                numberOfCurrentPage.current = 1;

                calculateImageCount();

                const message = {
                    type: MessageTypes.GET_THUMBNAILS,
                    thumbnailType: thumbnailTypeRef.current,
                    folderId: currentFolderIdRef.current,
                    page: numberOfCurrentPage.current.toString(),
                    size: numberOfThumbnailsOnPageRef.current.toString()
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
                }

                if (data.messageType === MessageTypes.INFO_RESPONSE) {
                    if (data.responseStatus === ResponseStatusTypes.UNSUPPORTED_MEDIA_TYPE) {
                        console.log('Unsupported media type received');
                        setImages((prevImages) => {
                            alert(`${texts.uploadFailure}`);
                            if (prevImages.length <= 0)
                                return prevImages
                            return prevImages[prevImages.length - 1].id === 0
                                ? prevImages.slice(0, prevImages.length - 1)
                                : prevImages;
                        });
                    } else if (data.responseStatus === ResponseStatusTypes.BAD_REQUEST) {
                        console.log('Bad request received');
                        alert(`${texts.badRequest}`);
                    }
                }

                if (data.messageType === MessageTypes.PLACEHOLDERS_NUMBER_RESPONSE) {
                    if (data.thumbnailsNumber !== null && data.thumbnailsNumber > 0) {
                        setIsLoading(false);
                        addIcons(data.thumbnailsNumber);
                        addThumbnails(thumbnailsMagazine);
                    }
                }

                if (data.messageType === MessageTypes.FOLDERS_RESPONSE) {
                    if (data.folderIds !== null) {
                        parentFolderIdRef.current = data.parentId;
                        addFolders(data.folders);
                    }
                }

                if (data.messageType === MessageTypes.DELETE_IMAGE_RESPONSE) {
                    if (data.id !== null) {
                        imagesRef.current = imagesRef.current.filter(folder => folder.id !== data.id);
                        setImages([...imagesRef.current]);
                    }
                }

                if (data.messageType === MessageTypes.GET_NEXT_PAGE) {
                    const pageable = data.pageable;
                    if (pageable && pageable.pageNumber !== undefined && pageable.pageNumber > numberOfCurrentPage.current) {
                        numberOfCurrentPage.current = pageable.pageNumber;
                        const message = {
                            type: MessageTypes.GET_THUMBNAILS,
                            thumbnailType: thumbnailTypeRef.current,
                            page: numberOfCurrentPage.current.toString(),
                            size: numberOfThumbnailsOnPageRef.current.toString(),
                            folderId: currentFolderIdRef.current
                        };
                        console.log('Sending message to server:', message);
                        ws.send(JSON.stringify(message));
                    } else {
                        if (pageable && pageable.pageNumber < numberOfCurrentPage.current) {
                            numberOfCurrentPage.current = pageable.pageNumber;
                        }
                        setTimeout(() => {
                            setIsLoading(false);
                            setIsObserverEnabled(true);
                        }, 3000);
                        //!TODO 3000 to configuration
                    }
                }

                if (data.messageType === MessageTypes.FETCHING_END_RESPONSE) {
                    setTimeout(() => {
                        setIsObserverEnabled(true);
                    }, 3000);
                    //!TODO same
                }

                if (data.messageType === MessageTypes.DELETE_FOLDER_RESPONSE) {
                    if (data.id !== null) {
                        foldersRef.current = foldersRef.current.filter(image => image.id !== data.id);
                        setFolder([...foldersRef.current]);
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

                if (data.messageType === MessageTypes.MOVE_IMAGE_RESPONSE) {
                    if (data.id !== null) {
                        setImages((prevImages) =>
                            prevImages.filter((image) => image.id !== data.id)
                        );
                    }
                }
            };

            ws.onclose = () => {
                console.log('Connection closed. Attempting to reconnect...');
                setConnectionStatus(ConnectionTypes.CONNECTING);
                if (!reconnectInterval) {
                    reconnectInterval = setInterval(() => {
                        connectWebSocket();
                    }, configuration.reconnecting_interval);
                }
            };

            ws.onerror = (error) => {
                console.error('Connection error:', error);
                setConnectionStatus(ConnectionTypes.DISCONNECTED);
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

    function calculateImageCount() {
        const screenWidth = window.innerWidth;
        const screenHeight = window.innerHeight;
        const imageSize = 170;
        const imagesPerRow =  Math.floor(screenWidth / imageSize);
        const rows = Math.floor(screenHeight / imageSize);
        numberOfThumbnailsOnPageRef.current = Math.floor(imagesPerRow * rows);
    }

    useEffect(() => {
        const handleResize = () => {
            calculateImageCount();
        };

        window.addEventListener("resize", handleResize);
        return () => {
            window.removeEventListener("resize", handleResize);
        };
    }, []);

    useEffect(() => {

        if (!isObserverEnabled) return;

        const observer = new IntersectionObserver(
            ([entry]) => {
                const hasScrollbar = document.documentElement.scrollHeight > window.innerHeight;

                if (entry.isIntersecting && hasScrollbar && connectionStatus === ConnectionTypes.CONNECTED) {
                    setIsLoading(true);
                    getNextPage();
                }
            },
            {
                root: null,
                threshold: 1.0,
                rootMargin: '10px',
            }
        );

        if (bottomRef.current) {
            observer.observe(bottomRef.current);
        }

        return () => {
            if (bottomRef.current) {
                observer.unobserve(bottomRef.current);
            }
        };
    }, [isObserverEnabled, connectionStatus]);

    const clearImages = () => {
        setImages([]);
    }

    const fetchThumbnails = () => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            const message = {
                type: MessageTypes.GET_THUMBNAILS,
                thumbnailType: thumbnailTypeRef.current,
                page: numberOfCurrentPage.current.toString(),
                size: numberOfThumbnailsOnPageRef.current.toString(),
                folderId: currentFolderIdRef.current
            };
            console.log('Sending message to server:', message);
            socket.send(JSON.stringify(message));
        }
    };

    const getNextPage = ()=> {
        if (socket && socket.readyState === WebSocket.OPEN) {
            setIsObserverEnabled(false);
            const message = {
                type: MessageTypes.GET_NEXT_PAGE,
                page: numberOfCurrentPage.current.toString(),
                size: numberOfThumbnailsOnPageRef.current.toString(),
                folderId: currentFolderIdRef.current
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
                if(thumbnail.data !== undefined && thumbnail.data !== null && thumbnail.iconOrder >= 0 && thumbnail.iconOrder < updatedImages.length){
                    updatedImages[thumbnail.iconOrder] = thumbnail;
                }
            }

            return updatedImages;
        });
    };

    const addFolders = (folders: Folder[]) => {
        setFolder(() => {
            const updatedFolders: ImageData[] = [];

            if (currentFolderIdRef.current != 0) {
                const newFolderIcon: ImageData = {
                    data: configuration.previousFolder,
                    id: parentFolderIdRef.current,
                    iconOrder: parentFolderIdRef.current,
                    name: texts.back
                };

                updatedFolders.push(newFolderIcon);
            }

            for (const folder of folders) {
                const newFolderIcon: ImageData = {
                    data: configuration.currentFolder,
                    id: folder.id,
                    iconOrder: folder.id,
                    name: folder.name,
                };

                updatedFolders.push(newFolderIcon);
            }

            return updatedFolders;
        });
    };

    const openMovePopup = () => {
        setShowMovePopup(true);
    };

    const handleMoveImages = (targetFolderId: number) => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            const message = {
                type: MessageTypes.MOVE_IMAGE,
                imageId: selectedImages,
                currentFolderId: currentFolderIdRef.current,
                targetFolderId: targetFolderId
            };
            console.log('Sending move image message to server:', message);
            socket.send(JSON.stringify(message));
        }
        setShowMovePopup(false);
        setSelectedImages([]);
    };

    return (
        <div>
            <header className="header">
                <h1 className="title">{texts.title}</h1>
                <div className="button-group">
                    <button className="button" onClick={() => setIsUploaderOpen(true)}>
                        {texts.addPhotos}
                    </button>
                    <button
                        className="button"
                        onClick={() => openMovePopup()}
                        disabled={selectedImages.length === 0}
                    >
                        {texts.moveImages}
                    </button>
                    {showMovePopup && (
                        <div className="move-popup">
                            <h3>{texts.selectDestinationFolder}</h3>
                            <ul>
                                {folders.map((folder) => (
                                    <li key={folder.id}>
                                        <button onClick={() => handleMoveImages(folder.id)}>
                                            {texts.folder + " " + folder.id}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                            <button onClick={() => setShowMovePopup(false)}>Close</button>
                        </div>
                    )}
                    <div className="thumbnail-buttons">
                        <button
                            className={`button ${thumbnailTypeRef.current === ThumbnailType.SMALL ? 'active' : ''}`}
                            onClick={() => {
                                thumbnailTypeRef.current = ThumbnailType.SMALL;
                                numberOfCurrentPage.current = 1;
                                clearImages();
                                fetchThumbnails();
                            }}
                        >{texts.small}
                        </button>
                        <button
                            className={`button ${thumbnailTypeRef.current === ThumbnailType.MEDIUM ? 'active' : ''}`}
                            onClick={() => {
                                thumbnailTypeRef.current = ThumbnailType.MEDIUM;
                                numberOfCurrentPage.current = 1;
                                clearImages();
                                fetchThumbnails();
                            }}
                        >{texts.medium}
                        </button>
                        <button
                            className={`button ${thumbnailTypeRef.current === ThumbnailType.BIG ? 'active' : ''}`}
                            onClick={() => {
                                thumbnailTypeRef.current = ThumbnailType.BIG;
                                numberOfCurrentPage.current = 1;
                                clearImages();
                                fetchThumbnails();
                            }}
                        >{texts.big}
                        </button>
                    </div>
                </div>
            </header>

            {connectionStatus === connectionTypes.CONNECTING && (
                <div className="connection-status">
                    <p>Connecting...</p>
                </div>
            )}
            {connectionStatus === connectionTypes.DISCONNECTED && (
                <div className="connection-status">
                    <p>Connection lost. Retrying...</p>
                </div>
            )}

            {isUploaderOpen && (
                <ImageUploader
                    onClose={() => setIsUploaderOpen(false)}
                    socket={socket}
                    page={numberOfCurrentPage.current}
                    size={numberOfThumbnailsOnPageRef.current}
                    currentFolder={currentFolderIdRef.current}
                />
            )}

            <ImageGallery
                images={images}
                folders={folders}
                originalImage={originalImage}
                socket={socket}
                setImages={setImages}
                setOriginalImage={setOriginalImage}
                thumbnailTypeRef={thumbnailTypeRef}
                currentFolderIdRef={currentFolderIdRef}
                numberOfThumbnailsRef={numberOfThumbnailsRef}
                numberOfThumbnailsOnPageRef={numberOfThumbnailsOnPageRef}
                numberOfCurrentPage={numberOfCurrentPage}
                onImageSelect={(imageId, isSelected) => {
                    setSelectedImages((prev) =>
                        isSelected ? [...prev, imageId] : prev.filter((id) => id !== imageId)
                    );
                }}
            />

            {isLoading && (
                <div className="spinner-container">
                    <Spinner />
                </div>
            )}

            <div ref={bottomRef} style={{height: '1px'}}></div>

        </div>
    );
}

export default App;
