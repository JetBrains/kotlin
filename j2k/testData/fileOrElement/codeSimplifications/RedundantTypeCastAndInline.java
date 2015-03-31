import java.lang.String;

class C {
    void foo(Object o) {
        if (o instanceof String) {
            String s = (String) o;
            int l = s.length();
            String substring = s.substring(l - 2);
        }
    }
}