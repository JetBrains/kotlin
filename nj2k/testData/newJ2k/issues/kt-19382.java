package test;

import java.util.HashMap;

public class TestPrimitiveFromMap {
    public int foo(HashMap<String, Integer> map) {
        return map.get("zzz");
    }
}