// LANGUAGE: +MultiPlatformProjects
// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// METADATA_ONLY_COMPILATION
// METADATA_TARGET_PLATFORMS: JVM, JS
// WITH_STDLIB
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
annotation class <!PARCELABLE_CANT_BE_ANNOTATION_CLASS!>BadAnnotation<!>

val badAnonymous = @Parcelize <!PARCELABLE_CANT_BE_ANONYMOUS_OBJECT!>object<!> {}

fun localCase() {
    @Parcelize
    class <!NO_PARCELABLE_SUPERTYPE, PARCELABLE_CANT_BE_LOCAL_CLASS!>LocalParcelable<!>(val id: Int)
}

@Parcelize
<!PARCELABLE_SHOULD_BE_INSTANTIABLE!>abstract<!> class AbstractParcelable(val id: Int)

@Parcelize
interface <!PARCELABLE_CANT_BE_NON_SEALED_INTERFACE!>NonSealedInterface<!>

@Parcelize
class <!PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY!>EmptyConstructor<!>()

@Parcelize
class BadParam(<!PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR!>id<!>: Int)

@Parcelize
class BadBodyProp(val id: Int) {
    var <!PROPERTY_WONT_BE_SERIALIZED!>unignored<!>: String = ""
}

@Parcelize
class Delegated(val shape: Shape) : Shape <!PARCELABLE_DELEGATE_IS_NOT_ALLOWED!>by<!> shape
