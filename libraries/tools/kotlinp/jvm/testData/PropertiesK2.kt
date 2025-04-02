// WITH_STDLIB
// IGNORE_BACKEND_K1: JVM_IR

annotation class Ann

class C(
    val constructorParam: String = "",
    @Ann val constructorValWithAnnotation: String = "",
    @get:Ann val constructorValWithGetterAnnotation: String = "",
    @Ann var constructorVarWithAnnotation: String = "",
    @get:Ann var constructorVarWithGetterAnnotation: String = "",
    @set:Ann var constructorVarWithSetterAnnotation: String = "",
    internal val constructorValWithInternal: String = "",
) {
    val getterOnlyVal: Double get() = 0.0
    var accessorOnlyVar: Int
        get() = 1
        set(value) {}

    var withBackingField: String = "42"

    val <T : Number> T.delegated: List<Nothing> by null

    val withOptimizedDelegate by C::getterOnlyVal

    operator fun Nothing?.getValue(x: Any?, y: Any?) = emptyList<Nothing>()


    internal var classVarWithInternal: String = ""
    var classVarWithSetterInternal: String = ""
        internal set

    @Ann val classValWithAnnotation: String = ""
    @get:Ann val classValWithGetterAnnotation: String = ""

    @Ann var classVarWithAnnotation: String = ""
    @get:Ann var classVarWithGetterAnnotation: String = ""
    @set:Ann var classVarWithSetterAnnotation: String = ""
}
