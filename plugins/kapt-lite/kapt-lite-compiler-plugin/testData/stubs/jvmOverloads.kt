// WITH_RUNTIME
package jvmOverloads

class User /** ctor */ @JvmOverloads constructor(val firstName: String = "", val secondName: String = "", val age: Int = 0) {
    /** foo */
    @JvmOverloads
    fun foo(a: Int, b: String, c: Long = 0) {}

    /** bar */
    fun bar(a: Int, b: String = "", c: Long = 0) {}

    /** baz */
    fun baz(a: Int = 0, b: String = "", c: Long = 0) {}
}