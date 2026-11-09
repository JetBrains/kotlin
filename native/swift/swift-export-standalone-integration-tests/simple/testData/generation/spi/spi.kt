// KIND: STANDALONE
// MODULE: lib
// EXPORT_TO_SWIFT
// FILE: annotations.kt

@RequiresOptIn(message = "This is an experimental API", level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
annotation class ExperimentalLibApi

@RequiresOptIn(message = "This is an internal API", level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
annotation class InternalLibApi

@RequiresOptIn(message = "An OptIn on an open class", level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class OpenClassOptIn

@RequiresOptIn(message = "An OptIn on an interface", level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class InterfaceOptInOne

@RequiresOptIn(message = "An OptIn on another interface", level = RequiresOptIn.Level.WARNING)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class InterfaceOptInTwo

// FILE: main.kt
@file:OptIn(ExperimentalLibApi::class, InternalLibApi::class)

@ExperimentalLibApi
class ExperimentalLibClass() {
    var foo: String = TODO()
    fun bar(): Unit = TODO()
    constructor(x: Int) : this()
}

@InternalLibApi
interface InternalLibInterface {
    var foo: String
    fun bar(): Unit

    @InternalLibApi
    fun internalFun(): Unit

    @InternalLibApi
    var internalProp: String

    @ExperimentalLibApi
    fun experimentalFun(): Unit

    @ExperimentalLibApi
    var experimentalProp: String
}

fun normalLibFunction(): Unit = TODO()

@ExperimentalLibApi
fun experimentalLibFunction(): Unit = TODO()

@InternalLibApi
fun internalLibFunction(): Unit = TODO()

val fooA: InternalLibInterface get() = TODO()

val fooB: ExperimentalLibClass get() = TODO()

@ExperimentalLibApi
var experimentalProperty: String
    get() = TODO()
    set(value) = TODO()

@InternalLibApi
var internalProperty: String
    get() = TODO()
    set(value) = TODO()

var experimentalLibSetter: String
    get() = TODO()
    @ExperimentalLibApi
    set(value) = TODO()

var internalLibSetter: String
    get() = TODO()
    @InternalLibApi
    set(value) = TODO()

context(a: InternalLibInterface)
fun fooA(b: ExperimentalLibClass): Unit = TODO()

fun InternalLibInterface.fooB(): ExperimentalLibClass = TODO()

fun fooC(): InternalLibInterface = TODO()

fun fooD(): ExperimentalLibClass = TODO()

@InternalLibApi
typealias InternalLibAlias = String

fun returnAlias(): InternalLibAlias = TODO()

class RegularLibClass @InternalLibApi constructor(a: String) {
    constructor() : this("")

    // A marked nested class whose outer declaration carries no marker of its own; only
    // the nested class is SPI, the outer stays public.
    @ExperimentalLibApi
    class MarkedNested {
        fun member(): String = TODO()
    }
}

val <T: InternalLibInterface> T.genericProperty: String get() = TODO()

fun <T: InternalLibInterface> genericFunction(a: T): String = TODO()

@OpenClassOptIn
open class OpenClass

@InterfaceOptInOne
interface InterfaceOne

@InterfaceOptInTwo
interface InterfaceTwo

// FILE: spi_extra.kt

@ExperimentalLibApi
abstract class SpiAbstractClass {
    abstract fun implementMe(): String
    fun template(): String = TODO()
}

// foo is marked in the base and its override has no marker of its own (the SPI arrives only
// through the overridden member), bar is plain in the base and its override is marked.
open class OverrideBase {
    @ExperimentalLibApi
    open fun foo(): String = TODO()
    open fun bar(): String = TODO()
}

@OptIn(ExperimentalLibApi::class)
open class OverrideDerived : OverrideBase() {
    override fun foo(): String = TODO()
    @ExperimentalLibApi
    override fun bar(): String = TODO()
}

// A typealias expands to the marked type and carries a type argument at once.
typealias ExperimentalClassList = List<ExperimentalLibClass>

@OptIn(ExperimentalLibApi::class)
fun returnsGenericAlias(): ExperimentalClassList = TODO()

@OptIn(ExperimentalLibApi::class)
fun mapOfSpi(): Map<String, ExperimentalLibClass> = TODO()

// FILE: first_group.kt

// Two markers with the same simple name in different packages are two distinct SPI groups: the
// derived names differ only in the package part ($-joined), so each declaration below carries
// its own group.
package spi.first

@RequiresOptIn
annotation class OptInMarker

@OptInMarker
fun firstGroupFunction(): Unit = TODO()

// FILE: second_group.kt

package spi.second

@RequiresOptIn
annotation class OptInMarker

@OptInMarker
fun secondGroupFunction(): Unit = TODO()

// MODULE: main(lib)
// EXPORT_TO_SWIFT
// FILE: annotations.kt

@RequiresOptIn(message = "This needs an OptIn")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
annotation class MyOptInApi

// FILE: main.kt
@file:OptIn(
    ExperimentalLibApi::class, InternalLibApi::class, MyOptInApi::class,
    OpenClassOptIn::class, InterfaceOptInOne::class, InterfaceOptInTwo::class,
)

@MyOptInApi
class MyOptInClass

@MyOptInApi
fun optInFunctionA(): Unit = TODO()

@ExperimentalLibApi
fun optInFunctionB(): Unit = TODO()

fun optInFunctionC(): ExperimentalLibClass = TODO()

typealias MyInterfaceAlias = InternalLibInterface

fun optInFunctionD(): MyInterfaceAlias = TODO()

typealias MyAliasAlias = InternalLibAlias

fun optInFunctionE(): MyAliasAlias = TODO()

@MyOptInApi
fun optInFunctionF() {
    fooD()
}

fun regularFunctionA() {
    experimentalLibFunction()
}

fun regularFunctionB() {
    internalLibFunction()
}

fun regularFunctionC(): RegularLibClass = TODO()

fun callbackFunction(action: () -> MyOptInClass): Unit = TODO()

var functionalTypePropertyA: (MyOptInClass) -> Unit
    get() = TODO()
    set(value) = TODO()

var functionalTypePropertyB: (InternalLibInterface) -> Unit
    get() = TODO()
    set(value) = TODO()

class MyImplementation : InternalLibInterface {
    override var foo: String = TODO()
    override fun bar(): Unit = TODO()

    @InternalLibApi
    override fun internalFun(): Unit = TODO()

    @InternalLibApi
    override var internalProp: String = TODO()

    @ExperimentalLibApi
    override fun experimentalFun(): Unit = TODO()

    @ExperimentalLibApi
    override var experimentalProp: String = TODO()
}

class MySubClass : OpenClass(), InterfaceOne

class MySubInterface : InterfaceTwo

interface MyInterface {
    var foo: String
    fun bar(): Unit

    @MyOptInApi
    fun optInFun(): Unit

    @MyOptInApi
    fun optInFunWithDefault(): Unit = TODO()

    @MyOptInApi
    var optInProp: String

    @MyOptInApi
    val defaultedProp: String get() = TODO()

    fun ExperimentalLibClass.bazFun(): Boolean

    var ExperimentalLibClass.bazProp: Boolean

    @MyOptInApi
    val String.optInExtPropWithDefault: String get() = TODO()
}

// FILE: internal_optin.kt

@RequiresOptIn(message = "This needs an OptIn")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR,
        AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS)
internal annotation class MyInternalOptInApi

@MyInternalOptInApi
public fun publicFunWithInternalOptIn(): String = TODO()
