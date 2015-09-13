package com.emmaguy.videopocket.video;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

class YouTubeVideoConverter implements Converter {
    private final Converter mOriginalConverter;

    YouTubeVideoConverter(Converter originalConverter) {
        mOriginalConverter = originalConverter;
    }

    @Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
        YouTubeVideoResponse response = (YouTubeVideoResponse) mOriginalConverter.fromBody(body, YouTubeVideoResponse.class);

        final List<YouTubeVideo> videoInfo = new ArrayList<>();
        for (YouTubeVideoResponse.YouTubeResponse r : response.getItems()) {
            videoInfo.add(new YouTubeVideo(r.getId(), r.getContentDetails().getDuration()));
        }

        return videoInfo;
    }

    @Override public TypedOutput toBody(Object object) {
        return mOriginalConverter.toBody(object);
    }
}
