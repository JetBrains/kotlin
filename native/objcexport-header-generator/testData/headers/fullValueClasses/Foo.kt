// LANGUAGE: +FullValueClasses
// ISSUE: KT-91357

value class Meters(val value: Int)

inline value class InlineMeters(val value: Int)

value class Point(val x: Int, val y: Int)

fun twice(meters: Meters): Meters = Meters(meters.value * 2)

fun twiceInline(meters: InlineMeters): InlineMeters = InlineMeters(meters.value * 2)

fun sum(point: Point): Int = point.x + point.y

fun nullable(meters: Meters?): Meters? = meters

fun nullableInline(meters: InlineMeters?): InlineMeters? = meters

fun list(meters: List<Meters>): List<Meters> = meters

fun listInline(meters: List<InlineMeters>): List<InlineMeters> = meters

fun Meters.doubled(): Int = value * 2

fun InlineMeters.doubledInline(): Int = value * 2

val zero: Meters get() = Meters(0)

val zeroInline: InlineMeters get() = InlineMeters(0)

fun map(meters: Meters, transform: (Meters) -> Meters): Meters = transform(meters)

fun mapInline(meters: InlineMeters, transform: (InlineMeters) -> InlineMeters): InlineMeters = transform(meters)

interface Marker

value class Tagged(val value: Int) : Marker

inline value class InlineTagged(val value: Int) : Marker

fun tag(tagged: Tagged): Tagged = tagged

fun tagInline(tagged: InlineTagged): InlineTagged = tagged
