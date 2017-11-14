
/** Test. */
class Test {
    /** method(). */
    fun method() {}

    /** method(int). */
    fun method(a: Int) {}

    /** method(String). */
    fun method(a: String) {}

    /** prop. */
    val prop: String = ""

    /** prop2. */
    @Anno
    val prop2: String = ""

    /** prop3. */
    var prop3: String
        /** get. */ get() = ""
        /** set. */ set(v) {}
}

/**
 * Test2
 * Multiline
 * documentation.
 */
class Test2(val a: String)

class Test3 /** constructor. */ protected constructor(val a: String)

/** Obj. */
object Obj

@Target(AnnotationTarget.PROPERTY)
annotation class Anno

/* simple comment. */
class Test4 {
    // method simple comment
    fun method() {}
}