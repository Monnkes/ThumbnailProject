import React from 'react';
import ReactDOM from 'react-dom/client';
import './styles/index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

const rootElement = document.getElementById('root');
if (rootElement === null) {
    throw new Error("Root not found");
}

const root = ReactDOM.createRoot(rootElement as HTMLElement);

root.render(
    <App />
);

reportWebVitals();
