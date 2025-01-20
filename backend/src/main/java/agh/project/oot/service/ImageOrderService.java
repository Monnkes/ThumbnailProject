package agh.project.oot.service;

import agh.project.oot.model.Folder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ImageOrderService {
    private final ConcurrentHashMap<Long, AtomicLong> folderImageOrders = new ConcurrentHashMap<>();
    private final FolderService folderService;
    private final ImageService imageService;
    private final ThumbnailService thumbnailService;

    public ImageOrderService(FolderService folderService, ImageService imageService, ThumbnailService thumbnailService) {
        this.folderService = folderService;
        this.imageService = imageService;
        this.thumbnailService = thumbnailService;
    }

    public Mono<Void> initializeImageOrder() {
        return folderService.getFolders()
                .map(Folder::getId)
                .flatMap(folderId -> imageService.findTopByFolderIdOrderByImageOrderDesc(folderId)
                        .mapNotNull(currentOrder -> folderImageOrders.put(folderId, new AtomicLong(currentOrder)))
                        .then())
                .then();
    }

    public Mono<Long> getNextImageOrder(Long folderId) {
        return Mono.fromCallable(() ->
                folderImageOrders.computeIfAbsent(folderId, id -> new AtomicLong(0))
                        .getAndIncrement());
    }

    public Mono<Void> recountImageOrder(Long imageId) {
        return imageService.getFolderIdByImageId(imageId)
                .flatMap(folderId -> {
                            folderImageOrders.put(folderId, new AtomicLong(0));
                            return recountImageOrder(folderId, imageId);
                        }
                );
    }

    public Mono<Void> recountImageOrder(Long folderId, Long imageId) {
        return imageService.getImagesByFolderId(folderId)
                .filter(image -> !Objects.equals(image.getId(), imageId))
                .flatMap(image ->
                        {
                            long newImageOrder = folderImageOrders.get(folderId).getAndIncrement();
                            return imageService.updateImageOrder(image, newImageOrder)
                                    .then(recountThumbnailOrder(image.getId(), newImageOrder));
                        }
                )
                .then();
    }

    public Mono<Void> recountThumbnailOrder(Long imageId, Long newOrderId) {
        System.err.println("New thumbnail order: " + newOrderId + "img: " + imageId);

        return thumbnailService.getAllThumbnailsByImageId(imageId)
                .flatMap(thumbnail ->
                        thumbnailService.updateThumbnailOrder(thumbnail, newOrderId)
                )
                .then();
    }

}
