// RUNTIME_WITH_FULL_JDK

public class Test {
    void m() {
        Double.isFinite(2.0);
        Double.isNaN(2.0);
        Float.isNaN(2.0f);
        Float.isInfinite(2.0f);
    }
}