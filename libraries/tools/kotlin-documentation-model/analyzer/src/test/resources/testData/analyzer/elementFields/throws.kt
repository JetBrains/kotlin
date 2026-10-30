// MODULE: main
// FILE: sample/Declarations.kt
package sample

class FirstException : Exception()
class SecondException : Exception()

/**
 * @throws FirstException documented and annotated
 * @exception SecondException documented only
 * @throws MissingException unresolved
 */
@Throws(FirstException::class, FirstException::class)
fun annotated() {}

class Constructed @Throws(FirstException::class) constructor()

/** @throws SecondException property documentation */
val documentedProperty: String = ""

@get:Throws(FirstException::class)
@set:Throws(SecondException::class)
var accessors: String = ""
    /** @throws FirstException getter documentation */
    get() = field
    /** @throws SecondException setter documentation */
    set(value) { field = value }

@Throws(java.io.IOException::class)
fun externalException() {}

fun plain() {}

// FILE: sample/JavaDeclarations.java
package sample;

public class JavaDeclarations {
    public JavaDeclarations() throws FirstException {}

    /**
     * @throws FirstException declared and documented
     * @exception SecondException documented only
     */
    public void declared() throws FirstException {}

    public void externalException() throws java.io.IOException {}

    public void plain() {}
}
