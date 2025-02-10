import React, {Dispatch, SetStateAction, useState, useEffect} from 'react';
import SelectedImage from './SelectedImage';
import './styles/ImageGallery.css';
import configuration from './frontendConfiguration.json';
import {ThumbnailSize, ThumbnailType} from "./utils/ThumbnailProperties";
import MessageTypes from "./utils/MessageTypes";
import texts from './texts/texts.json';

interface ImageGalleryProps {
    images: ImageData[];
    folders: ImageData[];
    originalImage: ImageData;
    socket: WebSocket | null;
    setImages: Dispatch<SetStateAction<ImageData[]>>;
    setOriginalImage: Dispatch<SetStateAction<ImageData>>;
    thumbnailTypeRef: React.MutableRefObject<ThumbnailType>;
    currentFolderIdRef: React.MutableRefObject<number>;
    numberOfThumbnailsRef: React.MutableRefObject<number>;
    numberOfThumbnailsOnPageRef: React.MutableRefObject<number>;
    numberOfCurrentPage: React.MutableRefObject<number>;
    onImageSelect: (imageId: number, isSelected: boolean) => void;
}

interface ImageData {
    data: string;
    id: number;
    iconOrder: number;
    name?: string;
}

const ImageGallery: React.FC<ImageGalleryProps> = ({
                                                       images,
                                                       folders,
                                                       originalImage,
                                                       socket,
                                                       setImages,
                                                       setOriginalImage,
                                                       thumbnailTypeRef,
                                                       currentFolderIdRef,
                                                       numberOfThumbnailsRef,
                                                       numberOfThumbnailsOnPageRef,
                                                       numberOfCurrentPage,
                                                       onImageSelect
                                                   }) => {
    const [showPopup, setShowPopup] = useState(false);
    const [selectedImage, setSelectedImage] = useState<ImageData | null>(null);
    const [checkboxStates, setCheckboxStates] = useState<{ [key: number]: boolean }>({});

    useEffect(() => {
        const initialCheckboxStates: { [key: number]: boolean } = {};
        images.forEach((image) => {
            initialCheckboxStates[image.id] = false;
        });
        setCheckboxStates(initialCheckboxStates);
    }, [images]);

    const handleImageClick = (image: string, index: number) => {
        setSelectedImage({data: image, id: index, iconOrder: index});
        setShowPopup(true);
    };

    const handleFolderClick = (folderId: number) => {
        setImages([]);
        numberOfThumbnailsRef.current = 0;
        currentFolderIdRef.current = folderId;
        numberOfCurrentPage.current = 1;

        const message = {
            type: MessageTypes.GET_THUMBNAILS,
            thumbnailType: thumbnailTypeRef.current,
            page: numberOfCurrentPage.current.toString(),
            size: numberOfThumbnailsOnPageRef.current.toString(),
            folderId: folderId
        };

        console.log('Sending message to server on folder click:', message);
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify(message));
        }
    };

    const handleDelete = (imageId: number) => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            const message = {
                type: MessageTypes.DELETE_IMAGE,
                id: imageId,
                size: numberOfThumbnailsOnPageRef.current.toString()
            };
            console.log('Sending delete message to server:', message);
            socket.send(JSON.stringify(message));
        }
    };

    const setCheckboxValue = (id: number, value: boolean) => {
        setCheckboxStates((prevState) => ({
            ...prevState,
            [id]: value,
        }));
    };

    const toggleCheckboxState = (id: number) => {
        setCheckboxStates((prevState) => ({
            ...prevState,
            [id]: !prevState[id],
        }));
    };

    const handleDeleteFolder = (folderId: number) => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            const message = {
                type: MessageTypes.DELETE_FOLDER,
                id: folderId,
                pageSize: numberOfThumbnailsOnPageRef.current.toString()
            };
            console.log('Sending delete folder to server:', message);
            socket.send(JSON.stringify(message));
        }
    }

    const handleMoveCheckboxChange = (imageId: number, isChecked: boolean) => {
        onImageSelect(imageId, isChecked);
    };

    const handleClosePopup = () => {
        setOriginalImage({data: configuration.loadingIcon, id: 0, iconOrder: -1});
        setShowPopup(false);
    };

    const getGridTemplateColumns = () => {
        switch (thumbnailTypeRef.current) {
            case ThumbnailType.SMALL:
                return `repeat(auto-fill, minmax(${ThumbnailSize[ThumbnailType.SMALL].height}, 1fr))`;
            case ThumbnailType.MEDIUM:
                return `repeat(auto-fill, minmax(${ThumbnailSize[ThumbnailType.MEDIUM].height}, 1fr))`;
            case ThumbnailType.BIG:
                return `repeat(auto-fill, minmax(${ThumbnailSize[ThumbnailType.BIG].height}, 1fr))`;
            default:
                return `repeat(auto-fill, minmax(${ThumbnailSize[ThumbnailType.SMALL].height}, 1fr))`;
        }
    };

    // TODO check the key must be '0' (should be base64Image.id)
    return (
        <div className="gallery" style={{gridTemplateColumns: getGridTemplateColumns()}}>
            {folders.map((folder, index) => (
                <div
                    key={0}
                    className="folder-container"
                    style={{
                        width: ThumbnailSize[thumbnailTypeRef.current].width,
                        height: ThumbnailSize[thumbnailTypeRef.current].height,
                        position: "relative",
                    }}
                >
                    <div className="folder">
                        <img
                            src={`data:image/png;base64,${folder.data}`}
                            alt={`Folder ${folder.id}`}
                            className="image"
                            onClick={() => handleFolderClick(folder.id)}
                        />
                        <div className="folder-name folder-label">
                            {folder.name}
                        </div>
                        <div className="folder-id folder-label">
                            {folder.id}
                        </div>
                    </div>
                    {(index !== 0 || currentFolderIdRef.current === 0) && (
                        <div className="folder-options">
                            <button
                                className="delete-button"
                                onClick={() => handleDeleteFolder(folder.id)}
                            >
                                {texts.cross}
                            </button>
                        </div>
                    )}
                </div>
            ))}
            {images.map((base64Image) => (
    <div
        key={0}
        className="image-container"
        style={{
            width: ThumbnailSize[thumbnailTypeRef.current].width,
            height: ThumbnailSize[thumbnailTypeRef.current].height,
            position: "relative",
        }}
    >
        <img
            src={base64Image.data ? `data:image/png;base64,${base64Image.data}` : configuration.loadingIcon}
            alt={`Uploaded ${base64Image.id}`}
            className="image"
            onClick={() => handleImageClick(base64Image.data, base64Image.id)}
        />
        {base64Image.id !== 0 && (
            <div className="image-options">
                <button
                    className="delete-button"
                    onClick={() => {
                        setCheckboxValue(base64Image.id, false);
                        onImageSelect(base64Image.id, false);
                        handleDelete(base64Image.id);
                    }}
                >
                    {texts.cross}
                </button>
                <input
                    className="move-checkbox"
                    type="checkbox"
                    id={base64Image.id.toString()}
                    checked={checkboxStates[base64Image.id]}
                    onChange={(e) => {
                        toggleCheckboxState(base64Image.id);
                        handleMoveCheckboxChange(base64Image.id, e.target.checked);
                    }}
                />
            </div>
        )}
    </div>
))}


            {showPopup && selectedImage && (
                <SelectedImage
                    imageData={selectedImage.data}
                    id={selectedImage.id}
                    originalImage={originalImage}
                    socket={socket}
                    onClose={handleClosePopup}
                />
            )}
        </div>
    );
};

export default ImageGallery;
