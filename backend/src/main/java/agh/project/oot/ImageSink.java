package agh.project.oot;

import lombok.Getter;
import reactor.core.publisher.Sinks;

@Getter
public class ImageSink {
    private final Sinks.Many<Long> sink;

    public ImageSink(int bufferSize) {
        this.sink = Sinks.many().multicast().onBackpressureBuffer(bufferSize);
    }

}