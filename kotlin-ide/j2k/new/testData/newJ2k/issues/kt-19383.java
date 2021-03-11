package test;

import java.util.Map;

public class TestMapGetAsReceiver {
    public int foo(Map<String, String> map) {
        return map.get("zzz").length();
    }
}