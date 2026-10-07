package com.transnacala.lory.utils;

import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class BooleanTypeAdapter extends TypeAdapter<Boolean> {

    @Override
    public void write(JsonWriter out, Boolean value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value);
        }
    }

    @Override
    public Boolean read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        if (token == JsonToken.NULL) {
            in.nextNull();
            return false;
        }
        if (token == JsonToken.BOOLEAN) {
            return in.nextBoolean();
        }
        if (token == JsonToken.NUMBER) {
            int value = in.nextInt();
            if (value == 0 || value == 1) {
                return value == 1;
            }
            throw new JsonSyntaxException("Expected boolean or MySQL boolean 0/1, got " + value);
        }
        if (token == JsonToken.STRING) {
            String value = in.nextString().trim();
            if ("1".equals(value) || "true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("0".equals(value) || "false".equalsIgnoreCase(value)) {
                return false;
            }
        }
        throw new JsonSyntaxException("Expected boolean or MySQL boolean 0/1 at " + in.getPath());
    }
}
