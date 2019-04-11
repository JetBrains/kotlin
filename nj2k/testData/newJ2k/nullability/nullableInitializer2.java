import org.jetbrains.annotations.Nullable;

public class TestJava {
    public Object nullableObj(int p) {
        return p > 0 ? "response" : null;
    }

    public String nullableInitializerFieldCast = (String) nullableObj(3);
    private String nullableInitializerPrivateFieldCast = (String) nullableObj(3);

    public void testProperty() {
        nullableInitializerFieldCast.charAt(0);
        nullableInitializerPrivateFieldCast.charAt(0);
    }

    public void testLocalVariable() {
        String nullableInitializerValCast = (String) nullableObj(3);

        nullableInitializerValCast.charAt(0);
    }
}