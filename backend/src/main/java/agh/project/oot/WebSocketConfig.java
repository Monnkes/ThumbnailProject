package agh.project.oot;

import agh.project.oot.controller.ThumbnailController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ThumbnailController thumbnailController;

    public WebSocketConfig(ThumbnailController thumbnailController) {
        this.thumbnailController = thumbnailController;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(thumbnailController, "/upload-files")
                .setAllowedOrigins("*");
    }
}
