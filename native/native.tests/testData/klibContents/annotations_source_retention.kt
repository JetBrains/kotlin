package test
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoClass
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoConstructor
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoConstructorParameter
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoProperty
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoSetParam
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoSetParam2
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoBackingField
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoGetter
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoSetter
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoSetter2
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoDelegatedField
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoFunction
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoFunctionParam
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoFunctionExtensionReceiver
@Retention(AnnotationRetention.SOURCE)
annotation class AnnoPropertyExtensionReceiver
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
