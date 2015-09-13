package com.emmaguy.videopocket.video;

import java.lang.reflect.Type;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

class VideoConverter implements Converter {
    private final Converter mOriginalConverter;

    VideoConverter(Converter originalConverter) {
        mOriginalConverter = originalConverter;
    }

    @Override public Object fromBody(TypedInput body, Type type) throws ConversionException {
        PocketVideoResponse response = (PocketVideoResponse) mOriginalConverter.fromBody(body, PocketVideoResponse.class);
        return response.getList();
    }

    @Override public TypedOutput toBody(Object object) {
        return mOriginalConverter.toBody(object);
    }
}
