// MODULE: main
// FILE: sample/Declarations.kt
package sample

/** @since release-one */
class Introduced /** @since release-two */ constructor() {
    @SinceKotlin("1.3")
    fun introducedFunction() {}
    /** @since release-three */
    val introducedProperty: String = ""
    fun plain() {}
}
/** @since release-four */
typealias IntroducedAlias = String
fun plain() {}

// FILE: sample/IntroducedJava.java
package sample;
/** @since java-one */
public class IntroducedJava {
    /** @since java-two */
    public IntroducedJava() {}
    /** @since java-three */
    public void introducedMethod() {}
    public void plain() {}
}
