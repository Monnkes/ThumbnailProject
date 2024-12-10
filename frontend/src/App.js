import React, { useState, useEffect } from 'react';
import ImageGallery from './ImageGallery';
import ImageUploader from './ImageUploader';
import './styles/App.css'

function App() {
     const [images, setImages] = useState([]);
     const [message, setMessage] = useState('');
     const [isUploaderOpen, setIsUploaderOpen] = useState(false);
     const [input, setInput] = useState('');
     const [socket, setSocket] = useState(null);


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

    const addImages = (newImages) => {
        setImages([...images, ...newImages]);
    };

    return (
      <div>
        <header className="header">
          <h1 className="title">Image Gallery</h1>
          <button className="button" onClick={() => setIsUploaderOpen(true)}>
            Add Photos
          </button>
        </header>
          {isUploaderOpen && (
              <ImageUploader
                  onClose={() => setIsUploaderOpen(false)}
                  onUpload={addImages}
                  socket={socket} // Przekazujemy WebSocket do komponentu
              />
          )}
        <ImageGallery images={images} />
      </div>
  );
}

export default App;
