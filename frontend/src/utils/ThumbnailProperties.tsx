export enum ThumbnailType{
    SMALL = "SMALL",
    MEDIUM = "MEDIUM",
    BIG = "BIG",
}

// !TODO Add 'width' and 'height" per size to configuration
export const ThumbnailSize = {
    [ThumbnailType.SMALL]: { width: '150px', height: '150px' },
    [ThumbnailType.MEDIUM]: { width: '300px', height: '300px' },
    [ThumbnailType.BIG]: { width: '600px', height: '600px' },
};