// TARGET_BACKEND: JS_IR, JS_IR_ES6, NATIVE
// LANGUAGE: +FullValueClasses
// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
inline value class InlineRadius(val radius: Int)

@Serializable
value class Radius(val radius: Int)

@Serializable
class Holder(val inlineRadius: InlineRadius, val inlineRadii: List<InlineRadius>, val radius: Radius, val radii: List<Radius>)

@Serializable
value class NullableRadius(val radius: Int?)

@Serializable
value class Box<T>(val value: T)

@Serializable
class GenericAndNullableHolder(
    val radius: Radius?,
    val absentRadius: Radius?,
    val nullableRadius: NullableRadius,
    val emptyNullableRadius: NullableRadius,
    val boxOfInt: Box<Int>,
    val boxOfRadius: Box<Radius>,
    val boxOfNullable: Box<String?>,
    val nullableBox: Box<Int>?,
)

@Serializable
inline value class InlineNullableRadius(val radius: Int?)

@Serializable
inline value class InlineBox<T>(val value: T)

@Serializable
class InlineGenericAndNullableHolder(
    val radius: InlineRadius?,
    val absentRadius: InlineRadius?,
    val nullableRadius: InlineNullableRadius,
    val emptyNullableRadius: InlineNullableRadius,
    val boxOfInt: InlineBox<Int>,
    val boxOfRadius: InlineBox<InlineRadius>,
    val nullableBox: InlineBox<Int>?,
)

@Serializable
class MixedHolder(val inlineBoxOfRadius: InlineBox<Radius>, val boxOfInlineRadius: Box<InlineRadius>)

@Serializable
value class DefaultRadius(val radius: Int = 1)

@Serializable
inline value class InlineDefaultRadius(val radius: Int = 1)

fun box(): String {
    // An inline value class is serialized as its underlying value
    val inlineRadius = InlineRadius(3)
    val encodedInlineRadius = Json.encodeToString(inlineRadius)
    val expectedInlineRadius = """3"""
    if (encodedInlineRadius != expectedInlineRadius) return "encoded inline radius: $encodedInlineRadius"
    if (Json.decodeFromString<InlineRadius>(encodedInlineRadius) != inlineRadius) return "decoded inline radius"

    // Off JVM, a single-field full value class without a supertype is serialized like an inline value class
    val radius = Radius(3)
    val encodedRadius = Json.encodeToString(radius)
    val expectedRadius = """3"""
    if (encodedRadius != expectedRadius) return "encoded radius: $encodedRadius"
    if (Json.decodeFromString<Radius>(encodedRadius) != radius) return "decoded radius"

    val holder = Holder(InlineRadius(1), listOf(InlineRadius(2), InlineRadius(3)), Radius(4), listOf(Radius(5), Radius(6)))
    val encodedHolder = Json.encodeToString(holder)
    val expectedHolder = """{"inlineRadius":1,"inlineRadii":[2,3],"radius":4,"radii":[5,6]}"""
    if (encodedHolder != expectedHolder) return "encoded holder: $encodedHolder"
    val decodedHolder = Json.decodeFromString<Holder>(encodedHolder)
    if (decodedHolder.inlineRadius != holder.inlineRadius || decodedHolder.inlineRadii != holder.inlineRadii) return "decoded inline radii"
    if (decodedHolder.radius != holder.radius || decodedHolder.radii != holder.radii) return "decoded radii"

    // The same holds for a full value class in a nullable position, with a nullable field and with a type parameter
    val nullableRadius = NullableRadius(null)
    val encodedNullableRadius = Json.encodeToString(nullableRadius)
    val expectedNullableRadius = """null"""
    if (encodedNullableRadius != expectedNullableRadius) return "encoded nullable radius: $encodedNullableRadius"
    if (Json.decodeFromString<NullableRadius>(encodedNullableRadius) != nullableRadius) return "decoded nullable radius"

    val box = Box(Radius(5))
    val encodedBox = Json.encodeToString(box)
    val expectedBox = """5"""
    if (encodedBox != expectedBox) return "encoded box: $encodedBox"
    if (Json.decodeFromString<Box<Radius>>(encodedBox) != box) return "decoded box"

    val genericAndNullableHolder = GenericAndNullableHolder(
        radius = Radius(1),
        absentRadius = null,
        nullableRadius = NullableRadius(2),
        emptyNullableRadius = NullableRadius(null),
        boxOfInt = Box(3),
        boxOfRadius = Box(Radius(4)),
        boxOfNullable = Box(null),
        nullableBox = null,
    )
    val encodedGenericAndNullableHolder = Json.encodeToString(genericAndNullableHolder)
    val expectedGenericAndNullableHolder =
        """{"radius":1,"absentRadius":null,"nullableRadius":2,"emptyNullableRadius":null,""" +
                """"boxOfInt":3,"boxOfRadius":4,"boxOfNullable":null,"nullableBox":null}"""
    if (encodedGenericAndNullableHolder != expectedGenericAndNullableHolder) {
        return "encoded generic and nullable holder: $encodedGenericAndNullableHolder"
    }
    val decoded = Json.decodeFromString<GenericAndNullableHolder>(encodedGenericAndNullableHolder)
    if (decoded.radius != genericAndNullableHolder.radius || decoded.absentRadius != null) return "decoded nullable radii"
    if (decoded.nullableRadius != genericAndNullableHolder.nullableRadius) return "decoded radius with nullable field"
    if (decoded.emptyNullableRadius != genericAndNullableHolder.emptyNullableRadius) return "decoded radius with null field"
    if (decoded.boxOfInt != genericAndNullableHolder.boxOfInt || decoded.boxOfRadius != genericAndNullableHolder.boxOfRadius) {
        return "decoded boxes"
    }
    if (decoded.boxOfNullable != genericAndNullableHolder.boxOfNullable || decoded.nullableBox != null) return "decoded nullable boxes"

    // An inline value class is serialized as its underlying value in a nullable position, with a nullable field and with a type parameter
    val inlineNullableRadius = InlineNullableRadius(null)
    val encodedInlineNullableRadius = Json.encodeToString(inlineNullableRadius)
    if (encodedInlineNullableRadius != """null""") return "encoded inline nullable radius: $encodedInlineNullableRadius"
    if (Json.decodeFromString<InlineNullableRadius>(encodedInlineNullableRadius) != inlineNullableRadius) return "decoded inline nullable radius"

    val inlineHolder = InlineGenericAndNullableHolder(
        radius = InlineRadius(1),
        absentRadius = null,
        nullableRadius = InlineNullableRadius(2),
        emptyNullableRadius = InlineNullableRadius(null),
        boxOfInt = InlineBox(3),
        boxOfRadius = InlineBox(InlineRadius(4)),
        nullableBox = null,
    )
    val encodedInlineHolder = Json.encodeToString(inlineHolder)
    val expectedInlineHolder =
        """{"radius":1,"absentRadius":null,"nullableRadius":2,"emptyNullableRadius":null,""" +
                """"boxOfInt":3,"boxOfRadius":4,"nullableBox":null}"""
    if (encodedInlineHolder != expectedInlineHolder) return "encoded inline holder: $encodedInlineHolder"
    val decodedInlineHolder = Json.decodeFromString<InlineGenericAndNullableHolder>(encodedInlineHolder)
    if (decodedInlineHolder.radius != inlineHolder.radius || decodedInlineHolder.absentRadius != null) return "decoded inline nullable radii"
    if (decodedInlineHolder.nullableRadius != inlineHolder.nullableRadius) return "decoded inline radius with nullable field"
    if (decodedInlineHolder.emptyNullableRadius != inlineHolder.emptyNullableRadius) return "decoded inline radius with null field"
    if (decodedInlineHolder.boxOfInt != inlineHolder.boxOfInt || decodedInlineHolder.boxOfRadius != inlineHolder.boxOfRadius) {
        return "decoded inline boxes"
    }
    if (decodedInlineHolder.nullableBox != null) return "decoded inline nullable box"

    // Off JVM, a full value class is unboxed inside an inline class and the other way around
    val mixedHolder = MixedHolder(InlineBox(Radius(1)), Box(InlineRadius(2)))
    val encodedMixedHolder = Json.encodeToString(mixedHolder)
    val expectedMixedHolder = """{"inlineBoxOfRadius":1,"boxOfInlineRadius":2}"""
    if (encodedMixedHolder != expectedMixedHolder) return "encoded mixed holder: $encodedMixedHolder"
    val decodedMixedHolder = Json.decodeFromString<MixedHolder>(encodedMixedHolder)
    if (decodedMixedHolder.inlineBoxOfRadius != mixedHolder.inlineBoxOfRadius) return "decoded inline box of radius"
    if (decodedMixedHolder.boxOfInlineRadius != mixedHolder.boxOfInlineRadius) return "decoded box of inline radius"

    if (!InlineRadius.serializer().descriptor.isInline) return "descriptor of inline radius is not inline"
    if (!Radius.serializer().descriptor.isInline) return "descriptor of radius is not inline"

    val encodedInlineDefaultRadius = Json.encodeToString(InlineDefaultRadius())
    if (encodedInlineDefaultRadius != """1""") return "encoded inline default radius: $encodedInlineDefaultRadius"
    if (Json.decodeFromString<InlineDefaultRadius>(encodedInlineDefaultRadius) != InlineDefaultRadius()) return "decoded inline default radius"

    // Off JVM, the default value of the only field of a full value class is encoded, like the one of an inline class
    val encodedDefaultRadius = Json.encodeToString(DefaultRadius())
    if (encodedDefaultRadius != """1""") return "encoded default radius: $encodedDefaultRadius"
    if (Json.decodeFromString<DefaultRadius>(encodedDefaultRadius) != DefaultRadius()) return "decoded default radius"

    return "OK"
}
