import React, { useEffect, useState } from 'react';

const WebSocketClient = () => {
    const [message, setMessage] = useState(''); // Przechowywanie wiadomości z serwera
    const [input, setInput] = useState('');    // Wiadomość do wysłania
    const [socket, setSocket] = useState(null);

    const dzejson = '{"type": "GetImages", "ids": [0, 1, 2, 3]}';
    useEffect(() => {
        // Tworzymy połączenie WebSocket
        const ws = new WebSocket('ws://localhost:8080/upload-files');

        ws.onopen = () => {
            console.log('Połączono z WebSocket');
        };

        ws.onmessage = (event) => {
            console.log('Otrzymano wiadomość:', event.data);
            setMessage(event.data); // Aktualizujemy wiadomość
        };

        ws.onclose = () => {
            console.log('WebSocket zamknięty');
        };

        ws.onerror = (error) => {
            console.error('Błąd WebSocket:', error);
        };

        setSocket(ws); // Zapisujemy instancję WebSocket

        // Sprzątanie po odłączeniu komponentu
        return () => {
            ws.close();
        };
    }, []); // Pusta tablica oznacza, że efekt uruchomi się tylko raz

    const sendMessage = () => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(input); // Wysyłamy wiadomość
            setInput(dzejson); // Czyścimy pole tekstowe
        } else {
            console.error('Połączenie WebSocket nie jest otwarte');
        }
    };

    return (
        <div>
            <h1>WebSocket Client</h1>
            <p>Otrzymana wiadomość: {message}</p>
            <input
                type="text"
                value={dzejson}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Wpisz wiadomość"
            />
            <button onClick={sendMessage}>Wyślij</button>
        </div>
    );
};

export default WebSocketClient;