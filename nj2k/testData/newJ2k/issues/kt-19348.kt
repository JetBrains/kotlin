class TestMutltipleCtorsWithJavadoc
/**
 * Javadoc for 1st ctor
 * @param x
 */(private val x: String) {
    private var y: String? = null

    // ---
    // Constructors
    //

    /**
     * Javadoc for 2nd ctor
     * @param x
     * @param y
     */
    constructor(x: String, y: String?) : this(x) {
        this.y = y
    }


}