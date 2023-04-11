// Most of this is not working on K2 now. Only most important parts are tested now.
// This should be fixed within KT-57929
package test

annotation class AnnoClass(val s: String)
annotation class AnnoConstructor(val s: String)
annotation class AnnoConstructorParameter(val s: String)
annotation class AnnoProperty(val s: String)
annotation class AnnoSetParam(val s: String)
annotation class AnnoSetParam2(val s: String)
annotation class AnnoBackingField(val s: String)
annotation class AnnoGetter(val s: String)
annotation class AnnoSetter(val s: String)
annotation class AnnoSetter2(val s: String)
annotation class AnnoDelegatedField(val s: String)
annotation class AnnoFunction(val s: String)
annotation class AnnoFunctionParam(val s: String)
annotation class AnnoFunctionExtensionReceiver(val s: String)
annotation class AnnoPropertyExtensionReceiver(val s: String)

@AnnoClass("O" + "K")
class Foo @AnnoConstructor("O" + "K") constructor(@AnnoConstructorParameter("O" + "K") i: Int) {
    @AnnoProperty("O" + "K")
    //@setparam:AnnoSetParam("O" + "K")
    //@field:AnnoBackingField("O" + "K")
    var prop: Int = i
        //@AnnoGetter("O" + "K")
        get() = field + 1
        //@AnnoSetter("O" + "K")
        set(x: Int) { field = x*2 }

    //@set:AnnoSetter2("O" + "K")
    var mutableProp = 0
        set(/*@AnnoSetParam2("O" + "K")*/ x: Int) { field = x*2 }

    //@delegate:AnnoDelegatedField("O" + "K")
    val immutableProp by lazy { prop }
}
@AnnoFunction("O" + "K")
fun /*@receiver:AnnoFunctionExtensionReceiver("O" + "K") */Foo.extfun(@AnnoFunctionParam("O" + "K") x: Int) {}
@AnnoPropertyExtensionReceiver("O" + "K")
val Foo.extProp get() = this.prop

