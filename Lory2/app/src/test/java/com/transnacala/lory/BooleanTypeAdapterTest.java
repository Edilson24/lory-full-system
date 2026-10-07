package com.transnacala.lory;

import com.google.gson.stream.JsonReader;
import com.transnacala.lory.utils.BooleanTypeAdapter;

import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BooleanTypeAdapterTest {

    private final BooleanTypeAdapter adapter = new BooleanTypeAdapter();

    @Test
    public void readsMysqlBooleanNumbers() throws Exception {
        assertFalse(adapter.read(new JsonReader(new StringReader("0"))));
        assertTrue(adapter.read(new JsonReader(new StringReader("1"))));
    }

    @Test
    public void readsJsonBooleans() throws Exception {
        assertFalse(adapter.read(new JsonReader(new StringReader("false"))));
        assertTrue(adapter.read(new JsonReader(new StringReader("true"))));
    }
}
