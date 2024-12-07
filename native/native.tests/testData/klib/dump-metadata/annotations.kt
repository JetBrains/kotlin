// FIR_IDENTICAL
package test

annotation class AnnoClass
annotation class AnnoConstructor
annotation class AnnoConstructorParameter
annotation class AnnoProperty
annotation class AnnoSetParam
annotation class AnnoSetParam2
annotation class AnnoBackingField
annotation class AnnoGetter
annotation class AnnoSetter
annotation class AnnoSetter2
annotation class AnnoDelegatedField
annotation class AnnoFunction
annotation class AnnoFunctionParam
annotation class AnnoFunctionExtensionReceiver
annotation class AnnoPropertyExtensionReceiver

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class AnnoFunctionTypeParameter
@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class AnnoClassTypeParameter
@Target(AnnotationTarget.TYPE)
annotation class AnnoClassUsageTypeParameter
@Target(AnnotationTarget.TYPE)
annotation class AnnoType

@AnnoClass
class Foo @AnnoConstructor constructor(@AnnoConstructorParameter i: Int) {
    @AnnoProperty
    @setparam:AnnoSetParam
    @field:AnnoBackingField
    var prop: Int = i
        @AnnoGetter
        get() = field + 1
        @AnnoSetter
        set(x: Int) { field = x*2 }

    @set:AnnoSetter2
    var mutableProp = 0
        set(@AnnoSetParam2 x: Int) { field = x*2 }

    @delegate:AnnoDelegatedField
    val immutableProp by lazy { prop }
}
@AnnoFunction
fun @receiver:AnnoFunctionExtensionReceiver Foo.extfun(@AnnoFunctionParam x: Int) {}
@AnnoPropertyExtensionReceiver
val Foo.extProp get() = this.prop

fun <@AnnoFunctionTypeParameter T> f(x : B<@AnnoClassUsageTypeParameter Int>) {}
class B<@AnnoClassTypeParameter T>

class Param
fun varargWithCustomTypeAnnotationOnParam(
    vararg v: @AnnoType Param
) {}
fun varargWithCustomTypeAnnotationOnInt(
    vararg v: @AnnoType Int
) {}
fun varargWithCustomTypeAnnotationOnLambda(
    vararg v: @AnnoType () -> Unit
) {}
fun varargWithExtensionFunctionTypeAnnotation(
    vararg v: Int.() -> Unit
) {}
fun varargWithCustomAndExtensionFunctionTypeAnnotation(
    vararg v: @AnnoType Int.() -> Unit
) {}
