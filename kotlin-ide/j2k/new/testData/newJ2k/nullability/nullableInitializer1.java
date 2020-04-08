import org.jetbrains.annotations.Nullable;

public class Test {
    public String nullableString(int p) {
        return p > 0 ? "response" : null;
    }

    private String nullableInitializerField = nullableString(3);
    private String nullableInitializerFieldFinal = nullableString(3);
    public String nullableInitializerPublicField = nullableString(3);

    public void testProperty() {
        nullableInitializerField = "aaa"

        nullableInitializerField.charAt(0);
        nullableInitializerFieldFinal.charAt(0);
        nullableInitializerPublicField.charAt(0);
    }

    public void testLocalVariable() {
        String nullableInitializerVal = nullableString(3);
        nullableInitializerVal.charAt(0);
    }
}