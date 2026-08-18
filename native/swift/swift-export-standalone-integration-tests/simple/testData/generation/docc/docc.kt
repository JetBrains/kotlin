// LANGUAGE: +ExportKDocDocumentationToKlib
// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

/**
* A class named `Foo`.
*
* Some more information about this class.
*
* @constructor The default constructor
* @property a A string property
* @property b An int property named `b`.
* Although in Swift this is named `z` (and we are testing a new line here).
* @param c A boolean constructor parameter
*
* @author Kodee
* @author Swift Export Team
*/
class Foo(
    val a: String,
    @ObjCName("z") val b: Int,
    c: Boolean,
) {

    /**
    * Secondary constructor without parameters
    */
    constructor() : this("", 0, false)

    /**
    * Secondary constructor with a single parameter
    * @param arg A regular parameter that accepts any object
    */
    constructor(arg: Any) : this("", 0, false)

    /**
    * Secondary constructor with some undocumented parameter/property
    */
    constructor(a: String) : this(a, 0, false)

    /**
    * A boolean property
    */
    var d: Boolean = true

    /**
    * A property with a context parameter
    * @param c A context parameter accepting any object
    */
    context(c: Any)
    var e: Int
        get() = 0
        set(_) = Unit

    /**
    * A readonly property
    */
    val f: String = "f"

    /**
    * A property with a receiver parameter
    * @receiver A string receiver parameter
    * @throws RuntimeException In case something goes wrong
    */
    val String.g: Int get() = TODO()

    /**
    * A function with a context and regular parameter
    * @param a A string context parameter
    * @param b A regular int parameter named `b`
    * @throws RuntimeException In case something goes wrong
    * @exception IllegalArgumentException In case `b` is negative
    */
    context(a: String)
    fun foo(@ObjCName("c") b: Int): Unit = TODO()

    /**
    * A funciton with multiple context parameters
    * @param a A string context parameter
    * @param b An int context parameter named `b`
    * @return A boolean
    * @see foo
    */
    context(a: String, @ObjCName("c") b: Int)
    fun bar(): Boolean = TODO()

    /**
    * A function with a receiver parameter
    * @receiver A string receiver parameter
    * @return An int value
    * @see bar for another function
    */
    fun String.baz(): Int = TODO()
}

/**
* An interface named `Bar`
*/
interface Bar {

    /**
    * A function inside an interface
    */
    fun foo()

    /**
    * A property inside an interface
    */
    var bar: String
}

/**
* An object named `Baz`
*/
object Baz {

    /**
    * This function does some internal things and should only be called by the library.
    * @suppress
    */
    fun someInternalLibFunction(): Unit = TODO()

    /**
    * Just a regular function
    * @myinfo Just some info with a custom tag
    */
    fun someNormalFunction(): String = TODO()

    // This is an undocumented function
    fun someUndocumentedFunction(): Int = TODO()
}
