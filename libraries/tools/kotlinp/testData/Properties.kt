class C(val constructorParam: String = "") {
    val getterOnlyVal: Double get() = 0.0
    var accessorOnlyVar: Int
        get() = 1
        set(value) {}

    var withBackingField: String = "42"

    val <T : Number> T.delegated: List<Nothing> by null

    val withOptimizedDelegate by C::getterOnlyVal

    operator fun Nothing?.getValue(x: Any?, y: Any?) = emptyList<Nothing>()
}
