import org.jetbrains.annotations.Nullable;

public class Test {
    public String notNullString(int p) {
        return "response";
    }

    private String notNullInitializerField = notNullString(3);
    public String notNullInitializerPublicField = notNullString(3);

    public void testProperty() {
        notNullInitializerField.charAt(0);
        notNullInitializerPublicField.charAt(0);
    }

    public void testLocalVariable() {
        String notNullInitializerVal = notNullString(3);
        notNullInitializerVal.charAt(0);
    }
}