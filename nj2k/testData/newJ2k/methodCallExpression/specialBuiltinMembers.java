import java.util.*;

enum E {
    A, B, C
}

class A {
    void foo(List<String> list, Collection<Integer> collection, Map<Integer, Integer> map) {
        int a = "".length();
        String b = E.A.name();
        int c = E.A.ordinal();
        int d = list.size() + collection.size();
        int e = map.size();
        int f = map.keySet().size();
        int g = map.values().size();
        int h = map.entrySet().size();
        int i = map.entrySet().iterator().next().getKey() + 1
    }

    void bar(List<String> list, HashMap<String, Integer> map) {
        char c = "a".charAt(0);
        byte b = new Integer(10).byteValue();
        int i = new Double(10.1).intValue();
        float f = new Double(10.1).floatValue();
        long l = new Double(10.1).longValue();
        short s = new Double(10.1).shortValue();

        try {
            String removed = list.remove(10);
            Boolean isRemoved = list.remove("a");
        }
        catch(Exception e) {
            System.err.println(e.getMessage());
            throw new RuntimeException(e.getCause());
        }

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            entry.setValue(value + 1);
        }
    }

    void kt21504() {
        byte b = Byte.parseByte("1");
        short s = Short.parseShort("1");
        int i  = Integer.parseInt("1");
        long l = Long.parseLong("1");
        float f = Float.parseFloat("1");
        double d = Double.parseDouble("1");

        byte b2 = Byte.parseByte("1", 10);
        short s2 = Short.parseShort("1", 10);
        int i2  = Integer.parseInt("1", 10);
        long l2 = Long.parseLong("1", 10);
    }
}
