internal class A(
// comment for field2 setter
        // comment for field2 getter
        var field2 // comment for field2
        : Int
) {
    /**
     * Comment for field1 setter
     */
    // Comment for field1 getter
    // Comment for field1
    var field1 = 0

    // comment for field3 setter
    // comment for field3 getter
    // comment before field3
    var field3 // comment for field3
            = 0

    // comment for setProperty
    // end of setProperty
    // comment for getProperty
    // end of getProperty
    var property: Int
        get() = 1
        set(value) {}

}