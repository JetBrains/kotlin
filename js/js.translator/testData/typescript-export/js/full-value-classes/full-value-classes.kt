// LANGUAGE: +FullValueClasses
// TSC_TARGET: es2020
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION
// WITH_STDLIB
// SKIP_NODE_JS
// INFER_MAIN_MODULE
// MODULE: JS_TESTS
// FILE: full-value-classes.kt

package foo

@JsExport
value class Point(val x: Int, val y: Int) {
    @JsName("createFromValue")
    constructor(value: Int) : this(value, value)

    fun sum(): Int = x + y

    @JsName("scaledSum")
    fun sum(scale: Int): Int = (x + y) * scale

    companion object {
        fun origin(): Point = Point(0, 0)
    }
}

@JsExport
interface PointConsumer {
    fun consume(point: Point): Int
}

@JsExport
class DefaultPointConsumer : PointConsumer {
    override fun consume(point: Point): Int = point.sum()
}

@JsExport
value class Labeled<T>(val value: T, val label: String)

@JsExport
fun createPoint(x: Int, y: Int): Point = Point(x, y)

@JsExport
fun acceptPoint(point: Point): Int = point.sum()

@JsExport
fun echoNullablePoint(point: Point?): Point? = point

@JsExport
fun echoPoints(points: Array<Point>): Array<Point> = points

@JsExport
fun <T> echoLabeled(value: Labeled<T>): Labeled<T> = value

@JsExport
suspend fun pointAsync(x: Int, y: Int): Point = Point(x, y)
