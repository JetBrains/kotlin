// This is an end-of-line comment

/*
This is a block comment
*/


/*doc comment of class*/
//one line comment of class
//another one
/*another doc*/
open class C() {
    // This is a class comment

    /**
     * This is a field doc comment.
     */
    private var i: Int = 0

    /**
     * This is a function doc comment.
     */
    public open fun foo() {
        /* This is a function comment */
    }

    //simple one line comment for function
    open fun f1() {
    }

    //simple one line comment for field
    var j: Int = 0

    //double c style
    //comment before function
    open fun f2() {
    }

    //double c style
    //comment before field
    var k: Int = 0

    //combination
    /** of
     */
    //
    /**
     * different
     */
    //comments
    open fun f3() {
    }

    //combination
    /** of
     */
    //
    /**
     * different
     */
    //comments
    var l: Int = 0

    /*two*/ /*comments*//*line*/
    var z: Int = 0
}