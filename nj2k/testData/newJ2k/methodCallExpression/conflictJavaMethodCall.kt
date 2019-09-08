// RUNTIME_WITH_FULL_JDK

class Test {
    internal fun m() {
        java.lang.Double.isFinite(2.0)
        java.lang.Double.isNaN(2.0)
        java.lang.Float.isNaN(2.0f)
        java.lang.Float.isInfinite(2.0f)
    }
}
