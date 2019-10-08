// This is an end-of-line comment
/*
This is a block comment
*/
/*doc comment of class*/ //one line comment of class
//another one
/*another doc*/
internal class C {
    // This is a class comment
    /**
     * This is a field doc comment.
     */
    private val i = 0

    /**
     * This is a function doc comment.
     */
    fun foo() { /* This is a function comment */
    }

    //simple one line comment for function
    fun f1() {}

    //simple one line comment for field
    var j = 0

    //double c style
//comment before function
    fun f2() {}

    //double c style
//comment before field
    var k = 0
    //combination
    /** of
     */
//
    /**
     * different
     */
//comments
    fun f3() {}
    //combination
    /** of
     */
//
    /**
     * different
     */
//comments
    var l = 0
    /*two*/ /*comments*/ /*line*/
    var z = 0
}