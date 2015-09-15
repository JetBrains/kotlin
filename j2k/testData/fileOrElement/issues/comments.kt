// This is an end-of-line comment

/*
This is a block comment
*/


/*doc comment of class*/
//one line comment of class
//another one
/*another doc*/
internal class C {
    // This is a class comment

    /**
     * This is a field doc comment.
     */
    private val i: Int = 0

    /**
     * This is a function doc comment.
     */
    fun foo() {
        /* This is a function comment */
    }

    //simple one line comment for function
    internal fun f1() {
    }

    //simple one line comment for field
    internal var j: Int = 0

    //double c style
    //comment before function
    internal fun f2() {
    }

    //double c style
    //comment before field
    internal var k: Int = 0

    //combination
    /** of
     */
    //
    /**
     * different
     */
    //comments
    internal fun f3() {
    }

    //combination
    /** of
     */
    //
    /**
     * different
     */
    //comments
    internal var l: Int = 0

    /*two*/ /*comments*//*line*/
    internal var z: Int = 0
}