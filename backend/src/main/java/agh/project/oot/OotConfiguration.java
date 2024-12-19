package agh.project.oot;

import agh.project.oot.controller.ThumbnailController;
import agh.project.oot.service.MessageService;
import agh.project.oot.service.ThumbnailService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class OotConfiguration implements WebSocketConfigurer {

    private final ThumbnailService thumbnailService;

    public OotConfiguration(ThumbnailService thumbnailService) {
        this.thumbnailService = thumbnailService;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
    }

    @Bean
    public MessageService messageService(ObjectMapper objectMapper) {
        return new MessageService(objectMapper, thumbnailService);
    }

    @Bean
    public ThumbnailController thumbnailController(MessageService messageService) {
        return new ThumbnailController(messageService);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(thumbnailController(messageService(objectMapper())), "/upload-files")
                .setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer(
            @Value("${configuration.maxTextMessageBufferSize}") int maxTextMessageBufferSize,
            @Value("${configuration.maxSessionIdleTimeout}") long maxSessionIdleTimeout) {

        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(maxTextMessageBufferSize);
        container.setMaxSessionIdleTimeout(maxSessionIdleTimeout);
        return container;
    }
}
