// MODULE: main
// FILE: sample/AnnotationTypes.kt
package sample

annotation class Marker
annotation class Levelled(val level: Int)
annotation class Composed(val first: Levelled, val message: String)

@Marker
fun marked() {}

@Levelled(42)
@Composed(Levelled(1), "context")
fun heavilyAnnotated(value: String) {}

fun markerParameter(value: Marker) {}
fun levelledParameter(value: Levelled) {}
fun nullableMarkerParameter(value: Marker?) {}
fun annotationList(values: List<Levelled>) {}
typealias MarkerAlias = Marker
fun viaAlias(value: MarkerAlias) {}

val annotatedGetter: String
    @Marker
    get() = ""

var annotatedAccessors: Int = 0
    @Marker
    get() = field
    @Marker
    set(@Marker value) {
        field = value
    }

fun Marker.annotatedReceiver() {}

class Carrying @Marker constructor(@Marker val value: Int) {
    @Marker
    fun method() {}
}
