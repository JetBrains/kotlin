package javadoc

/** Simple */
class A

/**
 * Multi
 * line
 * comment.
 */
class B {
    /** Nested
     * member
     * comment. */
    val a = ""

    /**
     * Mixed
     * tabs/spaces
     */
    val b = ""

    /**
     * List:
     * * first item
     * * second item
     */
    val c = ""

    /**
    Without
    stars
     */
    val d = ""

    /**
     * A mutable property
     */
    var e = ""

    /**
     * A property without a backing field
     */
    var f: String
        get() = ""
        set(value) {}

    /**
     * A property with a backing field and an explicit setter
     */
    var g: String
        set(value) {}

    /**
     * A property with documentation for an accessor
     */
    var h: String
        /**
         * It's a getter
         */
        get() = ""
        set(value) {}
}
