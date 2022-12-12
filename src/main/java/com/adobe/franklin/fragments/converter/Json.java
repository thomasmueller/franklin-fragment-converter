package com.adobe.franklin.fragments.converter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.adobe.franklin.fragments.utils.ProgressLogger;

/**
 * Wrapper around a JSONObject, which adds some convenience methods.
 */
public class Json {

    private final Map<String, Object> json;
    
    @SuppressWarnings("unchecked")
    public Json(JSONObject json) {
        this.json = (Map<String, Object>) json;
    }

    public Json getChild(String key) {
        return new Json((JSONObject) json.get(key));
    }
    
    public Map<String, Json> getChildren() {
        HashMap<String, Json> result = new HashMap<>();
        for(Entry<String, Object> e : json.entrySet()) {
            if (e.getValue() instanceof JSONObject) {
                result.put(e.getKey(), getChild(e.getKey()));
            }
        }
        return result;
    }
    
    public boolean containsKey(String key) {
        return json.containsKey(key);
    }
    
    public boolean isStringProperty(String key) {
        return json.get(key) instanceof String;
    }
    
    public boolean isArray(String key) {
        return json.get(key) instanceof JSONArray;
    }

    public String getStringProperty(String key) {
        return (String) json.get(key);
    }
    
    public void setStringProperty(String key, String value) {
        json.put(key, value);
    }

    
    public List<String> getStringArray(String key) {
        JSONArray list = (JSONArray) json.get(key);
        ArrayList<String> result = new ArrayList<>(list.size());
        for(int i=0; i < list.size(); i++) {
            result.add((String) list.get(i));
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public void setStringArray(String key, List<String> list) {
        JSONArray array = new JSONArray();
        array.addAll(list);
        json.put(key, array);
    }
    
    public String toString() {
        return json.toString();
    }

    public static Json parseFile(String jsonFileName) {
        try {
            ProgressLogger.logMessage("Reading JSON file");
            String json = new String(Files.readAllBytes(Paths.get(jsonFileName)));
            ProgressLogger.logDone();
            ProgressLogger.logMessage("Parsing JSON file");
            Json result = new Json((JSONObject) new JSONParser().parse(json));
            ProgressLogger.logDone();
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Json addChild(String key) {
        JSONObject obj = new JSONObject();
        json.put(key, obj);
        return new Json(obj);
    }

}
