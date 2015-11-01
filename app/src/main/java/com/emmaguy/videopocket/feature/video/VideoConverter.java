package com.emmaguy.videopocket.feature.video;

import java.lang.reflect.Type;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

class VideoConverter implements Converter {
    private final Converter converter;

    VideoConverter(Converter originalConverter) {
        this.converter = originalConverter;
    }

    @Override public Object fromBody(final TypedInput body, final Type type) throws ConversionException {
        if (type == ActionResultResponse.class) {
            return converter.fromBody(body, ActionResultResponse.class);
        }
        final PocketVideoResponse response = (PocketVideoResponse) converter.fromBody(body, PocketVideoResponse.class);
        return response.getList();
    }

    @Override public TypedOutput toBody(final Object object) {
        return converter.toBody(object);
    }
}
