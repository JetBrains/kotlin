internal class A(
// comment for field2 getter
        // comment for field2 setter
        var field2: Int // comment for field2
) {
    // Comment for field1
    // Comment for field1 getter
    /**
     * Comment for field1 setter
     */
    var field1 = 0

    // comment before field3
    var field3: Int = 0 // comment for field3
    // comment for field3 getter
    // comment for field3 setter

    // comment for getProperty
    // comment for setProperty
    var property: Int
        get() = 1
        set(value) {} // end of getProperty
    // end of setProperty
}