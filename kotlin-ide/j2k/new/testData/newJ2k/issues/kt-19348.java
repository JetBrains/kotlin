public class TestMutltipleCtorsWithJavadoc {
    private String x;
    private String y;

    // ---
    // Constructors
    //

    /**
     * Javadoc for 1st ctor
     * @param x
     */
    public TestMutltipleCtorsWithJavadoc(String x) {
        this.x = x;
    }

    /**
     * Javadoc for 2nd ctor
     * @param x
     * @param y
     */
    public TestMutltipleCtorsWithJavadoc(String x, String y) {
        this(x);
        this.y = y;
    }
}