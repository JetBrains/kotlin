// CHECK_TYPESCRIPT_DECLARATIONS
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// LANGUAGE: +ExportKDocDocumentationToKlib

// MODULE: JS_TESTS
// FILE: test.kt

/**
 * A simple exported function with just a description.
 */
@JsExport
fun simpleDescription(): String = TODO()

/**
 * Adds two numbers.
 *
 * This function performs **addition** of two integers.
 * See [Kotlin documentation](https://kotlinlang.org) for more details.
 *
 * @param a The first number.
 *   Must be a valid 32-bit integer.
 * @param b The second number.
 *   Must also be a valid 32-bit integer.
 * @return The sum of `a` and `b`
 */
@JsExport
fun add(a: Int, b: Int): Int = TODO()

/**
 * Divides two numbers.
 *
 * Performs *floating-point* division.
 *
 * ### Important
 *
 * If the divisor is zero, the behavior depends on the platform.
 *
 * @param dividend The dividend.
 *   This is the number to be divided.
 * @param divisor The divisor.
 *   This is the number by which `dividend` is divided.
 *   Must **not** be zero.
 * @return The result of dividing `dividend` by `divisor`.
 *   Returns `Infinity` for division by zero on some platforms.
 * @throws ArithmeticException if divisor is zero
 */
@JsExport
fun divide(dividend: Double, divisor: Double): Double = TODO()

/**
 * Parses a string to integer.
 * @param value The string value
 * @return The parsed integer
 * @exception NumberFormatException if not a valid integer
 */
@JsExport
fun parseIntValue(value: String): Int = TODO()

/**
 * Gets absolute value.
 * @param value Input number
 * @return The absolute value
 */
@JsExport
fun absolute(value: Int): Int = TODO()

/**
 * Legacy addition function.
 * @see add for the recommended approach
 * @since 1.0.0
 */
@JsExport
fun legacyAdd(a: Int, b: Int): Int = TODO()

/**
 * Old function.
 */
@JsExport
@Deprecated("Use newFunction instead")
fun oldFunction(): Unit = TODO()

/**
 * A function with a multiline description.
 *
 * This demonstrates how multiline KDoc descriptions
 * are translated into TSDoc format.
 *
 * Here is a code example:
 *
 * ```
 * val result = processText("hello")
 * println(result) // HELLO
 * ```
 *
 * And a list of transformations applied:
 * - Trim whitespace
 * - Convert to **uppercase**
 * - Encode to *UTF-8*
 *
 * @param input The input string.
 *   May contain any valid [Unicode](https://unicode.org) characters.
 * @return The processed result.
 *   Guaranteed to be non-empty if `input` is non-empty.
 */
@JsExport
fun processText(input: String): String = TODO()


/**
 * Represents a user.
 *
 * A data holder for user information used across the application.
 * Refer to the [User guide](https://example.com/users) for best practices.
 *
 * ### Example
 *
 * ```
 * val user = User("Alice", 30)
 * ```
 *
 * @property name The user's name.
 *   Should be a **non-empty** string.
 * @property age The user's age.
 *   Must be a *non-negative* integer.
 * @constructor Creates a new user instance with the given `name` and `age`.
 * @constructor This one should not appear in .d.ts
 */
@JsExport
class User(val name: String, var age: Int)

/**
 * A shape abstraction.
 *
 * @property readonlyProperty Is a special since for the Shape
 */
@JsExport
interface Shape {
    /**
     * Computes the area.
     * @return The area as a double
     */
    fun area(): Double

    /**
     * An internal property that should not appear in generated documentation.
     * @suppress
     */
    val readonlyProperty: String

    /**
     * A mutable property excluded from public API docs.
     * @suppress
     */
    var mutableProperty: String
}

/**
 * Math utility singleton.
 * @since 2.0.0
 */
@JsExport
object MathUtils {
    /**
     * The value of PI.
     */
    val PI: Double = TODO()

    /**
     * Computes circle circumference.
     * @param radius The circle radius
     * @return The circumference
     */
    fun circumference(radius: Double): Double = TODO()
}

/**
 * An interface with a property intended to be overridden with a custom JS name.
 * @property overridableProp A base property exposed under a custom JS name.
 * @property overridablePropWithCustomJsNameAccessors A base property whose getter and setter have custom JS names.
 */
@JsExport
interface WithOverridableProperty {
    /**
     * A property to be overridden with a custom JS name in the implementing class.
     */
    @JsName("jsOverridableProp")
    val overridableProp: String

    @get:JsName("overridableGetter")
    @set:JsName("overridableSetter")
    var overridablePropWithCustomJsNameAccessors: String
}

/**
 * Demonstrates @JsName applied to class properties in four distinct ways.
 * @property overridableProp An overridden property with a custom JS name.
 * @property originalNameProp A property renamed via @JsName on the property itself.
 * @property customAccessorProp A property with @JsName on its custom getter and setter.
 * @property defaultAccessorProp A property with @JsName on its default getter and setter.
 * @property overridablePropWithCustomJsNameAccessors An overridden property whose getter and setter have custom JS names.
 */
@JsExport
class JsNamePropertyExamples : WithOverridableProperty {
    /**
     * Case 1: @JsName on an overridden property.
     * The JavaScript name differs from the Kotlin override.
     */
    override val overridableProp: String = TODO()

    /**
     * Case 2: @JsName on the property itself (non-override, backing field).
     * Renames the generated JS getter.
     */
    @JsName("renamedProp")
    val originalNameProp: String = TODO()

    /**
     * Case 3: @JsName on custom getter and setter.
     * Each accessor is exposed under its own JS name.
     */
    var customAccessorProp: String
        @JsName("getCustomAccessor")
        get() = TODO()
        @JsName("setCustomAccessor")
        set(value) {}

    /**
     * Case 4: @JsName on the default getter and setter via use-site targets.
     * Both accessors are exposed as plain JS functions.
     */
    @get:JsName("getDefaultAccessor")
    @set:JsName("setDefaultAccessor")
    var defaultAccessorProp: String = TODO()

    override var overridablePropWithCustomJsNameAccessors: String
        get() = TODO()
        set(value) {}
}

/**
 * Finds the first matching element.
 *
 * Iterates through a predefined collection and returns
 * the **first** element satisfying the given `predicate`.
 *
 * ### Usage
 *
 * ```
 * val result = findFirst { it > 5 }
 * ```
 *
 * See also [MathUtils] for numerical utilities.
 *
 * @param predicate The matching condition.
 *   A lambda that receives an `Int` and returns `true`
 *   if the element matches.
 * @return The first match, or `null` if no element satisfies the predicate.
 *   Note: the return type is *nullable*.
 * @throws IllegalArgumentException if the internal list is empty
 * @see MathUtils for utility reference
 * @since 1.5.0
 */
@JsExport
fun findFirst(predicate: (Int) -> Boolean): Int? = TODO()

/**
 * @property name The user's name.
 */
@JsExport
class User2(val name: String)

/**
 * @param name Defined description by param tag.
 */
@JsExport
class User3(val name: String)

@JsExport
fun box(): String = "OK"
