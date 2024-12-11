import React, { useState, useEffect } from 'react';
import MessageTypes from "./MessageTypes";

interface WebSocketContextValue {
    socket: WebSocket | null;
    isConnected: boolean;
    message: JSON | null;
    addThumbnails: (thumbnails: string[]) => void;
}

interface WebSocketProviderProps {
    children: React.ReactNode;
    addThumbnails: (thumbnails: string[]) => void;
}

export const WebSocketContext = React.createContext<WebSocketContextValue | undefined>(undefined);

export const WebSocketProvider: React.FC<WebSocketProviderProps> = ({ children, addThumbnails }) => {
    const [socket, setSocket] = useState<WebSocket | null>(null);
    const [isConnected, setIsConnected] = useState(false);
    const [message, setMessage] = useState<JSON | null>(null);

    useEffect(() => {
        const ws = new WebSocket('ws://localhost:8080/upload-files');

        ws.onopen = () => {
            console.log('Connection established');
            setIsConnected(true);
        };

        ws.onmessage = (event) => {
            let receivedMessage = JSON.parse(event.data);
            console.log('Message received:', receivedMessage);
            setMessage(receivedMessage);

            const data = JSON.parse(event.data);
            if (data.type === MessageTypes.GetImagesResponse && data.imagesData) {
                addThumbnails(data.imagesData);
            }
        };

        ws.onclose = () => {
            console.log('Connection closed');
            setIsConnected(false);
        };

        ws.onerror = (error) => {
            console.error('Connection error :', error);
        };

        setSocket(ws);

        return () => {
            if (ws) {
                ws.close();
            }
        };
    }, [addThumbnails]);

    return (
        <WebSocketContext.Provider value={{ socket, isConnected, message, addThumbnails }}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = (): WebSocketContextValue => {
    const context = React.useContext(WebSocketContext);
    if (!context) {
        throw new Error('useWebSocket must be used within a WebSocketProvider');
    }
    return context;
};
