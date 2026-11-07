// MODULE: main
// FILE: sample/Declarations.kt
package sample

@Deprecated("Use NewClass", ReplaceWith("NewClass"), DeprecationLevel.ERROR)
class OldClass @Deprecated("Use factory") constructor() {
    @Deprecated("Use replacement")
    fun oldFunction() {}
    @Deprecated("Use another property")
    val oldProperty: String = ""
    var accessors: String = ""
        @Deprecated("Old getter") get
        @Deprecated("Old setter") set
}
class NewClass
@Deprecated("Use String")
typealias OldAlias = String
fun plain() {}

// FILE: sample/OldJava.java
package sample;
/** @deprecated Use NewClass instead. */
@Deprecated
public class OldJava {
    /** @deprecated Use another method. */
    public void oldMethod() {}
    public void plain() {}
}
