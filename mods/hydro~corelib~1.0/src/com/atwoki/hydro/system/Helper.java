package com.atwoki.hydro.system;

import org.vertx.java.core.MultiMap;
import org.vertx.java.core.json.JsonObject;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: ezra
 * Date: 7/17/12
 * Time: 11:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class Helper {

    public static String getID() {
        return UUID.randomUUID().toString();
    }

    public static StringBuffer jsonPrettyPrint(JsonObject json) {
        if (json != null && json.toMap() != null) {
            Map m = json.toMap();
            StringBuffer b = new StringBuffer();
            printMap(m, b, 0);
            return b;
        }
        return null;
    }

    public static StringBuffer mapPrettyPrint(Map map) {
        if (map != null) {
            StringBuffer b = new StringBuffer();
            printMap(map, b, 0);
            return b;
        }
        return null;
    }

    public static StringBuffer multiMapPrettyPrint(MultiMap map) {
        if (map != null) {
            StringBuffer b = new StringBuffer().append("[\n");
            for (String key : map.names()) {
                List<String> l = map.getAll(key);
                b.append("\t").append(key).append("=").append(l).append("\n");
            }
            return b.append("]");
        }
        return null;
    }

    private static void printMap(Map m, StringBuffer buffer, int level) {

        String tabs = "", comma;
        Iterator i;
        int items, count = 0;

        for (int t = 0; t < level; t++) {
            tabs += "\t";
        }
        level++;
        i = m.keySet().iterator();
        items = m.size();

        buffer.append(tabs).append("{\n");
        while (i.hasNext()) {
            ++count;
            if (count < items) {
                comma = ",";
            } else {
                comma = "";
            }
            Object k = i.next();
            Object v = m.get(k);
            buffer.append(tabs).append("\t\"").append(k).append("\": ");
            if (v != null) {
                if (v instanceof Map) {
                    printMap((Map) v, buffer, level);
                    buffer.append(comma).append("\n");
                } else if (v instanceof String) {
                    buffer.append("\"").append(v.toString()).append("\"").append(comma).append("\n");
                } else if (v instanceof Object []) {
                    printArray((Object []) v, buffer, level);
                    buffer.append(comma).append("\n");
                } else {
                    buffer.append(v.toString()).append(comma).append("\n");
                }
            }
        }
        buffer.append("\n").append(tabs).append("}");
    }

    private static void printArray(Object [] array, StringBuffer buffer, int level) {
        String comma;
        int items, count = 0;
        items = array.length;
        buffer.append("[").append("\n").append("\t");
        for (int i = 0; i < items; i++) {
            ++count;
            if (count < items) {
                comma = ",";
            } else {
                comma = "";
            }
            Object v = array[i];
            if (v != null) {
                if (v instanceof Map) {
                    printMap((Map) v, buffer, level);
                    buffer.append(comma).append("\n");
                } else if (v instanceof String) {
                    buffer.append("\"").append(v.toString()).append("\"").append(comma).append("\n");
                } else if (v instanceof Object []) {
                    printArray((Object []) v, buffer, level);
                    buffer.append(comma).append("\n");
                } else {
                    buffer.append(v.toString()).append(comma).append("\n");
                }
            }
        }
        buffer.append("]").append("\n");

        // TODO add code to add quotes to strings within an array e.g. buffer.append("\"" + object[i].toString() + "\"");
    }
}
