package com.emmaguy.videopocket.feature.video;

import android.support.annotation.NonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

class YouTubeVideoConverter implements Converter {
    private final Converter converter;

    YouTubeVideoConverter(@NonNull final Converter originalConverter) {
        this.converter = originalConverter;
    }

    @Override public Object fromBody(final TypedInput body, final Type type) throws ConversionException {
        final YouTubeVideoResponse response = (YouTubeVideoResponse) converter.fromBody(body, YouTubeVideoResponse.class);

        final List<YouTubeVideo> videoInfo = new ArrayList<>();
        for (YouTubeVideoResponse.YouTubeResponse r : response.getItems()) {
            videoInfo.add(new YouTubeVideo(r.getId(), r.getContentDetails().getDuration()));
        }

        return videoInfo;
    }

    @Override public TypedOutput toBody(final Object object) {
        return converter.toBody(object);
    }
}
