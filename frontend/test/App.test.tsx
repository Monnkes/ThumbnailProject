import {render, screen} from '@testing-library/react';
import App from '../src/App';
import React from 'react';

test('renders learn react link', () => {
    render(<App/>);
    const linkElement = screen.getByText(/Image Gallery/i);
    expect(linkElement).toBeInTheDocument();
});
