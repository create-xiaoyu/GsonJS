package com.xiaoyu.gsonjs;

import com.google.gson.*;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class GsonWrapper {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\['([^']+)']|\\.([A-Za-z0-9_]+)|\\[(\\d+)]|\\*|^([A-Za-z0-9_]+)");

    public String Analysis(String filePath, String jsonPath) {
        List<String> list = getJsonValueByPath(resolveToJsonString(filePath), jsonPath);
        if (list.isEmpty()) return "null";
        return list.getFirst();
    }

    public List<String> AnalysisAll(String filePath, String jsonPath) {
        return getJsonValueByPath(resolveToJsonString(filePath), jsonPath);
    }

    private String resolveToJsonString(String pathStr) {
        try {
            Path baseDir = FMLPaths.GAMEDIR.get().normalize().toAbsolutePath();
            Path filePath = baseDir.resolve(pathStr.replace('\\', '/')).normalize().toAbsolutePath();

            if (Files.exists(filePath)) {
                return Files.readString(filePath, StandardCharsets.UTF_8);
            }

            throw new IOException("File not found or invalid path: " + pathStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JSON source: " + pathStr, e);
        }
    }

    private List<String> getJsonValueByPath(String jsonString, String jsonParsingPath) {
        try {
            JsonElement root = JsonParser.parseString(jsonString);
            JsonElement result = get(root, jsonParsingPath);
            return flatten(result);
        } catch (Exception e) {
            GsonJS.LOGGER.error("Failed to parse JSON source: {}", jsonParsingPath, e);
            return List.of();
        }
    }

    private JsonElement get(JsonElement root, String path) {
        if (root == null || path == null || !path.startsWith("$"))
            throw new IllegalArgumentException("Invalid JSON path: " + path);

        path = path.substring(1);
        if (path.startsWith(".")) path = path.substring(1);
        List<Object> tokens = tokenize(path);
        return navigate(root, tokens);
    }

    private List<Object> tokenize(String path) {
        List<Object> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(path);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));
            } else if (matcher.group(2) != null) {
                tokens.add(matcher.group(2));
            } else if (matcher.group(3) != null) {
                tokens.add(Integer.parseInt(matcher.group(3)));
            } else if (matcher.group(4) != null) {
                tokens.add(matcher.group(4));
            } else {
                tokens.add("*");
            }
        }
        return tokens;
    }

    private JsonElement navigate(JsonElement current, List<Object> tokens) {
        for (Object token : tokens) {
            if (token.equals("*")) {
                JsonArray result = new JsonArray();
                if (current.isJsonObject()) {
                    for (var entry : current.getAsJsonObject().entrySet())
                        result.add(entry.getValue());
                } else if (current.isJsonArray()) {
                    for (JsonElement e : current.getAsJsonArray())
                        result.add(e);
                }
                current = result;
            } else if (token instanceof String key) {
                if (!current.isJsonObject()) return JsonNull.INSTANCE;
                JsonObject obj = current.getAsJsonObject();
                if (!obj.has(key)) return JsonNull.INSTANCE;
                current = obj.get(key);
            } else if (token instanceof Integer idx) {
                if (!current.isJsonArray()) return JsonNull.INSTANCE;
                JsonArray arr = current.getAsJsonArray();
                if (idx < 0 || idx >= arr.size()) return JsonNull.INSTANCE;
                current = arr.get(idx);
            }
        }
        return current;
    }

    private List<String> flatten(JsonElement element) {
        List<String> list = new ArrayList<>();
        if (element == null || element.isJsonNull()) return list;

        if (element.isJsonPrimitive()) {
            list.add(element.getAsString());
        } else if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray())
                list.addAll(flatten(e));
        } else if (element.isJsonObject()) {
            list.add(element.toString());
        }
        return list;
    }
}