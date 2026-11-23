// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// WITH_STDLIB
// DIAGNOSTICS: -ANNOTATION_CLASS_MEMBER

// MODULE: m1-common
// FILE: common.kt
@file:OptIn(kotlinx.parcelize.Experimental::class)

package test

import kotlinx.parcelize.DataClass
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

data class PlainData(val label: String)

expect class PlatformValue

@Parcelize
data class ValidUser(
    val name: String,
    val age: Int,
    val tags: List<String> = emptyList(),
    val pair: Pair<String, Int>,
    val failure: Throwable?,
    val builder: StringBuilder,
    val platformValue: PlatformValue,
    val rawOk: @RawValue Any,
    val plain: @DataClass PlainData,
) {
    @IgnoredOnParcel
    var transientNote: String = ""
}

@Parcelize
sealed interface Shape

@Parcelize
data class Circle(val radius: Double) : Shape

@Parcelize
sealed class CommonResult {
    @Parcelize
    data class Success(val user: ValidUser) : CommonResult()

    @Parcelize
    data object Empty : CommonResult()
}

@Parcelize
class ImplicitSupertype(val id: Int)

@Parcelize
enum class ImplicitEnum { A, B }

@Parcelize
object ImplicitObject

@Parcelize
annotation class <!PARCELABLE_CANT_BE_ANNOTATION_CLASS, PARCELABLE_CANT_BE_ANNOTATION_CLASS{METADATA}!>BadAnnotation<!>

val badAnonymous = @Parcelize <!PARCELABLE_CANT_BE_ANONYMOUS_OBJECT, PARCELABLE_CANT_BE_ANONYMOUS_OBJECT{METADATA}!>object<!> {}

fun localCase() {
    @Parcelize
    class <!NO_PARCELABLE_SUPERTYPE, NO_PARCELABLE_SUPERTYPE{METADATA}, PARCELABLE_CANT_BE_LOCAL_CLASS, PARCELABLE_CANT_BE_LOCAL_CLASS{METADATA}!>LocalParcelable<!>(val id: Int)
}

@Parcelize
<!PARCELABLE_SHOULD_BE_INSTANTIABLE, PARCELABLE_SHOULD_BE_INSTANTIABLE{METADATA}!>abstract<!> class AbstractParcelable(val id: Int)

@Parcelize
interface <!PARCELABLE_CANT_BE_NON_SEALED_INTERFACE, PARCELABLE_CANT_BE_NON_SEALED_INTERFACE{METADATA}!>NonSealedInterface<!>

@Parcelize
class <!PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY, PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY{METADATA}!>EmptyConstructor<!>()

@Parcelize
class BadParam(<!PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR, PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR{METADATA}!>id<!>: Int)

@Parcelize
class BadBodyProp(val id: Int) {
    var <!PROPERTY_WONT_BE_SERIALIZED, PROPERTY_WONT_BE_SERIALIZED{METADATA}!>unignored<!>: String = ""
}

@Parcelize
class Delegated(val shape: Shape) : Shape <!PARCELABLE_DELEGATE_IS_NOT_ALLOWED, PARCELABLE_DELEGATE_IS_NOT_ALLOWED{METADATA}!>by<!> shape

@Parcelize
expect class <!PARCELABLE_CANT_BE_EXPECT_DECLARATION, PARCELABLE_CANT_BE_EXPECT_DECLARATION{METADATA}!>ExpectParcelable<!>(id: Int) {
    val id: Int
}

@Parcelize
expect object <!PARCELABLE_CANT_BE_EXPECT_DECLARATION, PARCELABLE_CANT_BE_EXPECT_DECLARATION{METADATA}!>ExpectSingleton<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: android.kt
package test

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

actual typealias PlatformValue = String

@Parcelize
actual class ExpectParcelable actual constructor(actual val id: Int) : Parcelable

@Parcelize
actual object ExpectSingleton : Parcelable
