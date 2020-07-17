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

    void kt7940() {
        byte b1 = Byte.MIN_VALUE;
        byte b2 = Byte.MAX_VALUE;
        short s1 = Short.MIN_VALUE;
        short s2 = Short.MAX_VALUE;
        int i1 = Integer.MIN_VALUE;
        int i2 = Integer.MAX_VALUE;
        long l1 = Long.MIN_VALUE;
        long l2 = Long.MAX_VALUE;
        float f1 = Float.MIN_VALUE;
        float f2 = Float.MAX_VALUE;
        float f3 = Float.POSITIVE_INFINITY;
        float f4 = Float.NEGATIVE_INFINITY;
        float f5 = Float.NaN;
        double d1 = Double.MIN_VALUE;
        double d2 = Double.MAX_VALUE;
        double d3 = Double.POSITIVE_INFINITY;
        double d4 = Double.NEGATIVE_INFINITY;
        double d5 = Double.NaN;
    }

    void kt35593() {
        Number number = 1;
        byte b = number.byteValue();
        double d = number.doubleValue();
        float f = number.floatValue();
        int i = number.intValue();
        long l = number.longValue();
        short s = number.shortValue();
    }
}
