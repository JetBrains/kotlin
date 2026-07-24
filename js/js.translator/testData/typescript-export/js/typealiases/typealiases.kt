// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^^^^^
// The feature is supported only in the Analysis API based TypeScript export
// since IR doesn't have typealiases

// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// SKIP_NODE_JS
// WITH_STDLIB
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// LANGUAGE: +JsAllowExportTypealiases +JsExportingSuspendLambdas +NestedTypeAliases
// FILE: typealiases.kt

package foo

@JsExport
open class SomeClass(val value: String)

@JsExport
class GenericClass<T>(val value: T) {
  inner class Inner<S>
}

@JsExport
class GenericClassWithVariance<out T>(val value: T)

@JsExport
class TwoGenericParamsClass<A, B>(val first: A, val second: B)

@JsExport
interface SomeInterface {
    val prop: String
}

@JsExport
external interface SomeExternalInterface

@JsExport
enum class SomeEnum {
    A, B, C
}

@JsExport
object SomeObject {
    val value: String = "object"
}

@JsExport
public interface Comparable<in T> {
    operator fun compareTo(other: T): kotlin.Int
}

@JsExport
open class ClassWithConstraint<T : SomeClass>(val value: T) {
    inner class InnerWithConstraints<S : SomeEnum>
}

@JsExport
open class RecursiveBoundClass<T : Comparable<T>>(val value: T)

// --- Primitive type aliases ---

@JsExport
typealias MyInt = Int

@JsExport
typealias MyString = String

@JsExport
typealias MyBoolean = Boolean

@JsExport
typealias MyDouble = Double

@JsExport
typealias MyByte = Byte

@JsExport
typealias MyShort = Short

@JsExport
typealias MyFloat = Float

@JsExport
typealias MyChar = Char

@JsExport
typealias MyAny = Any

// --- Nullable type aliases ---

@JsExport
typealias NullableInt = Int?

@JsExport
typealias NullableString = String?

@JsExport
typealias NullableAny = Any?

// --- Unsigned type aliases ---

@JsExport
typealias MyUByte = UByte

@JsExport
typealias MyUShort = UShort

@JsExport
typealias MyUInt = UInt

// --- Array type aliases ---

@JsExport
typealias MyIntArray = IntArray

@JsExport
typealias MyBooleanArray = BooleanArray

@JsExport
typealias MyStringArray = Array<String>

@JsExport
typealias MyNullableIntArray = IntArray?

@JsExport
typealias NestedArray = Array<Array<String>>

// --- Class/Interface/Enum/Object type aliases ---

@JsExport
typealias AliasSomeClass = SomeClass

@JsExport
typealias AliasSomeInterface = SomeInterface

@JsExport
typealias AliasSomeExternalInterface = SomeExternalInterface

@JsExport
typealias AliasSomeEnum = SomeEnum

@JsExport
typealias NullableSomeClass = SomeClass?

// --- Generic type aliases (non-generic alias pointing to a concrete generic type) ---

@JsExport
typealias ConcreteGenericClass = GenericClass<String>

@JsExport
typealias ConcreteGenericClassInt = GenericClass<Int>

@JsExport
typealias ConcreteTwoGenericParamsClass = TwoGenericParamsClass<String, Int>

// --- Generic type aliases (alias with type parameters) ---

@JsExport
typealias AliasGenericClass<T> = GenericClass<T>

@JsExport
typealias AliasTwoGenericParamsClass<A, B> = TwoGenericParamsClass<A, B>

@JsExport
typealias FlippedTwoGenericParamsClass<A, B> = TwoGenericParamsClass<B, A>

@JsExport
typealias PartiallySpecializedGenericClass<T> = TwoGenericParamsClass<String, T>

// --- Generic type alias with constraint ---

@JsExport
typealias AliasClassWithConstraint<T> = ClassWithConstraint<T>

@JsExport
typealias AliasClassWithMultipleConstraints<T> = Pair<RecursiveBoundClass<T>, ClassWithConstraint<T>.InnerWithConstraints<T>>

// Constraint inherited from an F-bound (exercises rewriting the bound in terms of the alias's parameter):
@JsExport
typealias AliasRecursiveBound<T> = RecursiveBoundClass<T>

// Constraint inherited through an alias-to-alias chain (exercises full expansion):
@JsExport
typealias AliasOfConstrainedAlias<T> = AliasClassWithConstraint<T>

// --- Nullable generic type aliases ---

@JsExport
typealias NullableGenericClass<T> = GenericClass<T>?

@JsExport
typealias GenericClassNullableParam<T> = GenericClass<T?>

// --- Function type aliases ---

@JsExport
typealias SimpleCallback = () -> Unit

@JsExport
typealias IntToString = (Int) -> String

@JsExport
typealias BinaryOperation = (Int, Int) -> Int

@JsExport
typealias GenericTransformer<T, R> = (T) -> R

@JsExport
typealias NullableCallback = (() -> Unit)?

@JsExport
typealias CallbackWithNullableParam = (Int?) -> String?

@JsExport
typealias HigherOrderFunction = ((Int) -> String) -> Int

@JsExport
typealias CurriedFunction = (Int) -> (String) -> Boolean

@JsExport
typealias SuspendCallback = suspend () -> Unit

@JsExport
typealias SuspendTransformer<T, R> = suspend (T) -> R

// --- Typealias to typealias ---

@JsExport
typealias AliasOfMyInt = MyInt

@JsExport
typealias AliasOfAliasSomeClass = AliasSomeClass

@JsExport
typealias AliasOfGenericAlias<T> = AliasGenericClass<T>

// --- Throwable type alias ---

@JsExport
typealias MyThrowable = Throwable

@JsExport
typealias NullableThrowable = Throwable?

// --- Nothing type alias ---

@JsExport
typealias MyNothing = Nothing

// --- Unit type alias ---

@JsExport
typealias MyUnit = Unit

// --- Nested typealiases (inside classes) ---

@JsExport
class ClassWithNestedTypealiases {
    typealias NestedInt = Int
    typealias NestedString = String
    typealias NestedClassAlias = SomeClass
    typealias NestedGenericAlias<T> = GenericClass<T>
    typealias NestedConcreteGenericAlias = GenericClass<String>
    typealias NestedCallback = () -> Unit
    typealias NestedNullable = Int?

    typealias Self = ClassWithNestedTypealiases
}

@JsExport
class GenericClassWithNestedTypealiases<T> {
    typealias NestedAlias = Int
    typealias NestedGenericAlias<T, R> = TwoGenericParamsClass<T, R>

    typealias Self<T> = GenericClassWithNestedTypealiases<T>
}

// --- Nested typealiases (inside interfaces) ---

@JsExport
interface InterfaceWithNestedTypealiases {
    typealias InterfaceNestedInt = Int
    typealias InterfaceNestedClassAlias = SomeClass
    typealias InterfaceNestedGenericAlias<T> = GenericClass<T>
    typealias InterfaceNestedCallback = (Int) -> String
    typealias Self = InterfaceWithNestedTypealiases
}

// --- Nested typealiases (inside objects) ---

@JsExport
object ObjectWithNestedTypealiases {
    typealias ObjectNestedInt = Int
    typealias ObjectNestedClassAlias = SomeClass
    typealias ObjectNestedGenericAlias<T> = GenericClass<T>
    typealias Self = ObjectWithNestedTypealiases
}

// --- Nested typealiases (inside companion objects) ---

@JsExport
class ClassWithCompanionTypealiases {
    companion object {
        typealias CompanionNestedInt = Int
        typealias CompanionNestedString = String
        typealias CompanionNestedGenericAlias<T> = GenericClass<T>
        typealias Self = ClassWithCompanionTypealiases.Companion
    }
}

@JsExport
class ClassWithNamedCompanionTypealiases {
    companion object Named {
        typealias NamedCompanionNestedAlias = String
        typealias NamedCompanionGenericAlias<T> = GenericClass<T>
        typealias Self = ClassWithNamedCompanionTypealiases.Named
    }
}

// --- Nested typealiases (inside enum classes) ---

@JsExport
enum class EnumWithNestedTypealiases {
    X, Y, Z;

    typealias EnumNestedInt = Int
    typealias EnumNestedClassAlias = SomeClass
    typealias Self = EnumWithNestedTypealiases
}

// --- Nested typealiases (inside open/abstract classes) ---

@JsExport
open class OpenClassWithNestedTypealiases {
    typealias OpenClassNestedAlias = String
    typealias OpenClassNestedGenericAlias<T> = GenericClass<T>
}

@JsExport
abstract class AbstractClassWithNestedTypealiases {
    typealias AbstractNestedAlias = Int
    typealias AbstractNestedClassAlias = SomeClass
}

// --- Nested typealiases (deeply nested in class within class) ---

@JsExport
class OuterWithNested {
    class InnerNested {
        typealias DeeplyNestedAlias = Int
        typealias DeeplyNestedClassAlias = SomeClass
    }

    typealias OuterNestedAlias = String
}

// --- Nested typealiases (inside sealed class/interface) ---

@JsExport
sealed class SealedClassWithNestedTypealiases {
    typealias SealedNestedAlias = Int
    class Sub : SealedClassWithNestedTypealiases()
}

@JsExport
sealed interface SealedInterfaceWithNestedTypealiases {
    typealias SealedInterfaceNestedAlias = String
    class Impl : SealedInterfaceWithNestedTypealiases
}

// --- Nested typealias with @JsExport.Ignore ---

@JsExport
class ClassWithIgnoredNestedTypealias {
    typealias VisibleAlias = Int

    @JsExport.Ignore
    typealias IgnoredNestedAlias = String
}

// --- @JsExport.Ignore on typealias ---

@JsExport.Ignore
typealias IgnoredAlias = String

// --- Typealiases used in exported declarations ---
// FILE: usage.kt

package foo

@JsExport
fun consumeMyInt(value: MyInt): MyInt = value + 1

@JsExport
fun consumeCallback(cb: SimpleCallback?) = cb?.invoke()

@JsExport
fun consumeGenericAlias(value: AliasGenericClass<String>): String = value.value

@JsExport
class ClassUsingAlias(val id: MyInt, val name: MyString)

@JsExport
typealias AliasWithVarianceInAliasedType<T> = GenericClass<in T>

@JsExport
typealias AliasWithVarianceInsideOfAliasedType<T> = GenericClassWithVariance<T>

// Type alias to an inner class and references to it:
@JsExport
typealias AliasWithInnerClass<T, S> = GenericClass<T>.Inner<S>

@JsExport
fun acceptInnerAlias(value: AliasWithInnerClass<Int, String>) {}

