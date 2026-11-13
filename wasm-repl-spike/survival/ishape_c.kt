// v2: Shape gained a method. Does an old (v1) Sq still pass `is Shape`,
// and what happens when v2 code calls the new method through the old itable?

interface Shape {
    fun area(): Int
    fun name(): String
}

fun isShapeV2(x: Any?): Boolean = x is Shape

fun areaOf(x: Any?): Int = (x as? Shape)?.area() ?: -1

fun nameOf(x: Any?): String = (x as? Shape)?.name() ?: "<null>"
