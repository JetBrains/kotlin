// WITH_STDLIB

// Trivial enums: should be optimized to a plain number
enum class Day {
    MONDAY,
    TUESDAY,
    WEDNESDAY
}

enum class HttpCode(val code: Int) {
    NOT_FOUND(404),
    FORBIDDEN(403)
}

// Non-trivial enums: should stay regular classes
enum class Shape(val sides: Int) {
    TRIANGLE(3) {
        override fun describe(): String = "triangle"
    },
    SQUARE(4) {
        override fun describe(): String = "square"
    };

    abstract fun describe(): String
}

enum class Operation(val symbol: String, val priority: Int) {
    PLUS("+", 1),
    TIMES("*", 2)
}

fun testTrivialDay(day: Day): String = "${day.name}:${day.ordinal}"

fun testTrivialHttpCode(httpCode: HttpCode): Int = httpCode.code

fun testTrivialEquals(a: Day, b: Day): Boolean = a == b

fun testNonTrivialShape(shape: Shape): String = "${shape.name}:${shape.ordinal}:${shape.sides}:${shape.describe()}"

fun testNonTrivialOperation(operation: Operation): String = "${operation.symbol}:${operation.priority}"

fun testNonTrivialEquals(a: Shape, b: Shape): Boolean = a == b

// Access all the `Enum` members through an upcast to `Enum`
fun <E : Enum<E>> testUpcastToEnum(value: Enum<E>, other: E): String {
    val name = value.name
    val ordinal = value.ordinal
    val string = value.toString()
    val hashCode = value.hashCode()
    val equalsSelf = value.equals(value)
    val equalsOther = value.equals(other)
    val compareToOther = value.compareTo(other).coerceIn(-1, 1)
    return "$name:$ordinal:$string:$hashCode:$equalsSelf:$equalsOther:$compareToOther"
}

fun testUpcastToEnumNullable(value: Enum<*>?): String = value?.name ?: "null"

fun testUpcastToEnumList(values: List<Enum<*>>): String = values.joinToString(",") { "${it.name}:${it.ordinal}" }

fun box(): String {
    if (testTrivialDay(Day.MONDAY) != "MONDAY:0") return "Fail testTrivialDay(MONDAY): ${testTrivialDay(Day.MONDAY)}"
    if (testTrivialDay(Day.WEDNESDAY) != "WEDNESDAY:2") return "Fail testTrivialDay(WEDNESDAY): ${testTrivialDay(Day.WEDNESDAY)}"
    if (testTrivialHttpCode(HttpCode.NOT_FOUND) != 404) return "Fail testTrivialHttpCode(NOT_FOUND)"
    if (testTrivialHttpCode(HttpCode.FORBIDDEN) != 403) return "Fail testTrivialHttpCode(FORBIDDEN)"
    if (!testTrivialEquals(Day.TUESDAY, Day.TUESDAY)) return "Fail testTrivialEquals(TUESDAY, TUESDAY)"
    if (testTrivialEquals(Day.MONDAY, Day.TUESDAY)) return "Fail testTrivialEquals(MONDAY, TUESDAY)"

    if (Day.values().joinToString(",") { it.name } != "MONDAY,TUESDAY,WEDNESDAY") return "Fail Day.values()"
    if (Day.entries.joinToString(",") { it.ordinal.toString() } != "0,1,2") return "Fail Day.entries"
    if (HttpCode.valueOf("FORBIDDEN").code != 403) return "Fail HttpCode.valueOf(FORBIDDEN)"
    if (enumValueOf<Day>("TUESDAY") != Day.TUESDAY) return "Fail enumValueOf<Day>(TUESDAY)"

    val boxed: Any = Day.TUESDAY
    if (boxed !is Day || boxed != Day.TUESDAY) return "Fail boxed Day"
    if (boxed.toString() != "TUESDAY") return "Fail boxed Day.toString(): $boxed"
    if (Day.TUESDAY.hashCode() != 1) return "Fail Day.hashCode(): ${Day.TUESDAY.hashCode()}"
    if (Day.MONDAY.compareTo(Day.WEDNESDAY) >= 0) return "Fail Day.compareTo"

    val upcastDay = testUpcastToEnum(Day.TUESDAY, Day.WEDNESDAY)
    if (upcastDay != "TUESDAY:1:TUESDAY:1:true:false:-1") return "Fail testUpcastToEnum(Day.TUESDAY, Day.WEDNESDAY): $upcastDay"
    val upcastDaySame = testUpcastToEnum(Day.WEDNESDAY, Day.WEDNESDAY)
    if (upcastDaySame != "WEDNESDAY:2:WEDNESDAY:2:true:true:0") return "Fail testUpcastToEnum(Day.WEDNESDAY, Day.WEDNESDAY): $upcastDaySame"
    val upcastHttpCode = testUpcastToEnum(HttpCode.FORBIDDEN, HttpCode.NOT_FOUND)
    if (upcastHttpCode != "FORBIDDEN:1:FORBIDDEN:1:true:false:1") return "Fail testUpcastToEnum(HttpCode.FORBIDDEN, HttpCode.NOT_FOUND): $upcastHttpCode"
    if (testUpcastToEnumNullable(Day.MONDAY) != "MONDAY") return "Fail testUpcastToEnumNullable(Day.MONDAY)"
    if (testUpcastToEnumNullable(null) != "null") return "Fail testUpcastToEnumNullable(null)"
    val upcastList = testUpcastToEnumList(listOf(Day.MONDAY, HttpCode.FORBIDDEN, Shape.SQUARE, Operation.PLUS))
    if (upcastList != "MONDAY:0,FORBIDDEN:1,SQUARE:1,PLUS:0") return "Fail testUpcastToEnumList: $upcastList"
    val upcastNonTrivial = testUpcastToEnum(Shape.TRIANGLE, Shape.SQUARE)
    if (!upcastNonTrivial.startsWith("TRIANGLE:0:TRIANGLE:") || !upcastNonTrivial.endsWith(":true:false:-1")) return "Fail testUpcastToEnum(Shape.TRIANGLE, Shape.SQUARE): $upcastNonTrivial"

    if (testNonTrivialShape(Shape.TRIANGLE) != "TRIANGLE:0:3:triangle") return "Fail testNonTrivialShape(TRIANGLE): ${testNonTrivialShape(Shape.TRIANGLE)}"
    if (testNonTrivialShape(Shape.SQUARE) != "SQUARE:1:4:square") return "Fail testNonTrivialShape(SQUARE): ${testNonTrivialShape(Shape.SQUARE)}"
    if (testNonTrivialOperation(Operation.TIMES) != "*:2") return "Fail testNonTrivialOperation(TIMES): ${testNonTrivialOperation(Operation.TIMES)}"
    if (!testNonTrivialEquals(Shape.SQUARE, Shape.SQUARE)) return "Fail testNonTrivialEquals(SQUARE, SQUARE)"
    if (testNonTrivialEquals(Shape.TRIANGLE, Shape.SQUARE)) return "Fail testNonTrivialEquals(TRIANGLE, SQUARE)"
    if (Shape.valueOf("SQUARE") !== Shape.SQUARE) return "Fail Shape.valueOf(SQUARE)"

    return "OK"
}
