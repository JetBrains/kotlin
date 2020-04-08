import org.jetbrains.annotations.Nullable;

public class TestJava {
    private String notNullInitializerFieldNullableUsage = "aaa";
    private String notNullInitializerFieldNotNullUsage = "aaa";

    private String nullInitializerFieldNullableUsage = null;
    private String nullInitializerFieldNotNullUsage = null;

    public void testNotNull(@Nullable Object obj) {
        if (true) {
            notNullInitializerFieldNullableUsage = (String) obj;
            notNullInitializerFieldNotNullUsage = "str";

            notNullInitializerFieldNullableUsage.charAt(1);
            notNullInitializerFieldNotNullUsage.charAt(1);
        }
        else {
            nullInitializerFieldNullableUsage = (String) obj;
            nullInitializerFieldNotNullUsage = "str";

            nullInitializerFieldNullableUsage.charAt(1);
            nullInitializerFieldNotNullUsage.charAt(1);
        }
    }
}