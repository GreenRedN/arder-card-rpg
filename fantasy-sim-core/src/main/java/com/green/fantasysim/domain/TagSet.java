package com.green.fantasysim.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class TagSet {
    private final LinkedHashMap<String, String> map = new LinkedHashMap<>();
    public TagSet put(String k, String v) { map.put(k, v); return this; }
    public String get(String k) { return map.get(k); }
    public Set<Map.Entry<String,String>> entries() { return map.entrySet(); }
    public String signature() {
        StringBuilder sb = new StringBuilder();
        for (var e : map.entrySet()) sb.append(e.getKey()).append("=").append(e.getValue()).append(";");
        return sb.toString();
    }
}
