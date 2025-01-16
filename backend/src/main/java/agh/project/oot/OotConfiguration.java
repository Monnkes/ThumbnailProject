package agh.project.oot;

import agh.project.oot.controller.ThumbnailController;
import agh.project.oot.repository.ThumbnailRepository;
import agh.project.oot.service.ImageService;
import agh.project.oot.service.MessageService;
import agh.project.oot.service.ThumbnailService;
import agh.project.oot.thumbnails.ThumbnailConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableScheduling
public class OotConfiguration {

    @Bean
    public ThumbnailController thumbnailController(@Lazy MessageService messageService, SessionRepository sessionManager) {
        return new ThumbnailController(messageService, sessionManager);
    }

    @Bean
    public ImageSink imageSink(@Value("${configuration.backpressureBuffer}") int buffer) {
        return new ImageSink(buffer);
    }

    @Bean
    public ThumbnailService thumbnailService(ThumbnailConverter thumbnailConverter,
                                             ImageService imageService,
                                             ThumbnailRepository thumbnailRepository) {
        return new ThumbnailService(thumbnailConverter, imageService, thumbnailRepository);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
    }

    @Bean
    public MessageService messageService(ObjectMapper objectMapper,
                                         ImageSink imageSink,
                                         ThumbnailService thumbnailService,
                                         ImageService imageService,
                                         SessionRepository sessionRepository) {
        return new MessageService(objectMapper, imageSink, thumbnailService, imageService, sessionRepository);
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