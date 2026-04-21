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
class ExperimentalLibClass {
    var foo: String = TODO()
    fun bar(): Unit = TODO()
}

@InternalLibApi
interface InternalLibInterface {
    var foo: String
    fun bar(): Unit
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
}

val <T: InternalLibInterface> T.genericProperty: String get() = TODO()

fun <T: InternalLibInterface> genericFunction(a: T): String = TODO()

@OpenClassOptIn
open class OpenClass

@InterfaceOptInOne
interface InterfaceOne

@InterfaceOptInTwo
interface InterfaceTwo

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
}

class MySubClass : OpenClass(), InterfaceOne

class MySubInterface : InterfaceTwo
