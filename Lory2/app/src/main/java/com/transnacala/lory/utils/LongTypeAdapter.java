package com.transnacala.lory.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LongTypeAdapter extends TypeAdapter<Long> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    public void write(JsonWriter out, Long value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value);
        }
    }

    @Override
    public Long read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return 0L;
        }

        if (in.peek() == JsonToken.NUMBER) {
            return in.nextLong();
        }

        if (in.peek() == JsonToken.STRING) {
            String str = in.nextString();
            if (str == null || str.trim().isEmpty()) {
                return 0L;
            }
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException e) {
                try {
                    Date date = dateFormat.parse(str);
                    if (date != null) {
                        return date.getTime();
                    }
                } catch (Exception ignored) {
                }
            }
        } else {
            in.skipValue();
        }

        return System.currentTimeMillis();
    }
}