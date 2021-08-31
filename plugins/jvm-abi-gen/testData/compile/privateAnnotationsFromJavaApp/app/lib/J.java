package lib;

@A(value = "OK")
public class J {
    public static String value() {
        return J.class.getAnnotation(A.class).value();
    }
}
