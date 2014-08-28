package com.kryptnostic.api.v1.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.kryptnostic.kodex.v1.serialization.jackson.KodexModule;

/**
 * A {@link Converter} which uses Jackson for reading and writing entities.
 *
 * @author Kai Waldron (kaiwaldron@gmail.com)
 */
public class JacksonConverter implements Converter {
    private static final String MIME_TYPE = "application/json; charset=UTF-8";

    private final ObjectMapper objectMapper;

    public JacksonConverter() {
        this(new ObjectMapper());
    }

    public JacksonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        objectMapper.registerModule(new GuavaModule());
        objectMapper.registerModule(new KodexModule());
        objectMapper.registerModule(new AfterburnerModule());
    }

    @Override
    public Object fromBody(TypedInput body, Type type) throws ConversionException {
        try {
            JavaType javaType = objectMapper.getTypeFactory().constructType(type);
            InputStream in = body.in();
            if (in.available() == 0) {
                return null;
            }
            return objectMapper.readValue(body.in(), javaType);
        } catch (JsonParseException e) {
            throw new ConversionException(e);
        } catch (JsonMappingException e) {
            throw new ConversionException(e);
        } catch (IOException e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public TypedOutput toBody(Object object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            return new TypedByteArray(MIME_TYPE, json.getBytes("UTF-8"));
        } catch (JsonProcessingException e) {
            throw new AssertionError(e);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}