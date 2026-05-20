package com.syos.common;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Handles JSON serialization/deserialization and socket message framing.
 * Uses newline-delimited JSON protocol over TCP sockets.
 * 
 * Concurrency note: Each client connection has its own reader/writer,
 * but writes must be synchronized to prevent interleaved messages.
 */
public class JsonProtocol {

    private static final Gson GSON = createGson();

    private static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .serializeNulls()
                .create();
    }

    public static Gson getGson() {
        return GSON;
    }

    /** Serialize a Request to JSON string */
    public static String toJson(Request request) {
        return GSON.toJson(request);
    }

    /** Serialize a Response to JSON string */
    public static String toJson(Response response) {
        return GSON.toJson(response);
    }

    /** Deserialize JSON string to Request */
    public static Request parseRequest(String json) {
        return GSON.fromJson(json, Request.class);
    }

    /** Deserialize JSON string to Response */
    public static Response parseResponse(String json) {
        return GSON.fromJson(json, Response.class);
    }

    /** Convert any object to JsonElement for embedding in Response */
    public static JsonElement toJsonElement(Object obj) {
        return GSON.toJsonTree(obj);
    }

    /** Send a message (request or response) over the socket as a single line */
    public static void sendMessage(PrintWriter writer, String json) {
        synchronized (writer) {
            writer.println(json);
            writer.flush();
        }
    }

    /** Read a single line message from the socket */
    public static String readMessage(BufferedReader reader) throws IOException {
        return reader.readLine();
    }

    /** Create a BufferedReader for a socket */
    public static BufferedReader createReader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
    }

    /** Create a PrintWriter for a socket */
    public static PrintWriter createWriter(Socket socket) throws IOException {
        return new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
    }

    // ==================== Custom Type Adapters ====================

    /**
     * Gson TypeAdapter for java.time.LocalDate.
     * Serializes as ISO-8601 date string (yyyy-MM-dd).
     */
    private static class LocalDateAdapter extends TypeAdapter<LocalDate> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

        @Override
        public void write(JsonWriter out, LocalDate value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(FORMATTER));
            }
        }

        @Override
        public LocalDate read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDate.parse(in.nextString(), FORMATTER);
        }
    }

    /**
     * Gson TypeAdapter for java.time.LocalDateTime.
     * Serializes as ISO-8601 date-time string.
     */
    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(FORMATTER));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            return LocalDateTime.parse(in.nextString(), FORMATTER);
        }
    }
}
