// DONT_TARGET_EXACT_BACKEND: JS
// DONT_TARGET_EXACT_BACKEND: WASM
// ES_MODULES

@OptIn(ExperimentalStdlibApi::class)
@JsExternalInheritorsOnly
external interface ExternalInterfaceX {
    val x: String
}

external interface ExternalInterfaceXY : ExternalInterfaceX {
    val y: String
}

external interface ExternalInterfaceXYZ : ExternalInterfaceXY {
    val z: String
}

external class ExternalXYZ() : ExternalInterfaceXYZ {
    override val x: String
    override val y: String
    override val z: String
}

external class ExternalClassNameSpace {
    interface NestedInterfaceXYZ : ExternalInterfaceXYZ {
        override val x: String
        override val y: String
        override val z: String
    }
}

@JsModule("./jsExternalInheritorsOnly.mjs")
external object Creator: ExternalInterfaceXYZ {
    fun createX(): ExternalInterfaceX
    fun createXY(): ExternalInterfaceXY
    fun createXYZ(): ExternalInterfaceXYZ
    fun createClassXYZ(): ExternalXYZ

    fun createNestedInterfaceXYZ(): ExternalClassNameSpace.NestedInterfaceXYZ

    override val x: String
    override val y: String
    override val z: String
}

fun checkX(x: ExternalInterfaceX, id: Int) = x.x == "X$id"
fun checkXY(xy: ExternalInterfaceXY, id: Int) = checkX(xy, id) && xy.y == "Y$id"
fun checkXYZ(xyz: ExternalInterfaceXYZ, id: Int) = checkXY(xyz, id) && xyz.z == "Z$id"

fun box(): String {
    if (!checkX(Creator.createX(), 1)) return "Fail interface X"
    if (!checkXY(Creator.createXY(), 2)) return "Fail interface XY"
    if (!checkXYZ(Creator.createXYZ(), 3)) return "Fail interface XYZ"

    if (!checkXYZ(Creator.createClassXYZ(), 4)) return "Fail class XYZ"

    if (!checkXYZ(Creator.createNestedInterfaceXYZ(), 5)) return "Fail nested interface XYZ"

    if (!checkXYZ(Creator, 6)) return "Fail object XYZ"

    return "OK"
}
