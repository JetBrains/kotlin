// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// LANGUAGE: +JvmInlineMultiFieldValueClasses

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.internal.*

@Serializable
sealed interface I

@Serializable
@JvmInline
value class DPoint(val x: Double, val y: Double): I

@Serializable
@JvmInline
value class DSegment(val p1: DPoint, val p2: DPoint): I

@Serializable
data class PointWrapper(val value: DPoint)

@Serializable
data class SegmentWrapper(val value: DSegment)

fun box(): String {
    val p1 = DPoint(1.0, 2.0)
    val dSegment = DSegment(p1, DPoint(3.0, 4.0))
    run {
        val s = Json.encodeToString(DPoint.serializer(), p1)
        if (s != """{"x":1.0,"y":2.0}""") return s
        val decoded = Json.decodeFromString(DPoint.serializer(), s)
        if (p1 != decoded) return decoded.toString()
    }
    run {
        val s = Json.encodeToString(DSegment.serializer(), dSegment)
        if (s != """{"p1":{"x":1.0,"y":2.0},"p2":{"x":3.0,"y":4.0}}""") return s
        val decoded = Json.decodeFromString(DSegment.serializer(), s)
        if (dSegment != decoded) return decoded.toString()
    }
    run {
        val pointWrapper = PointWrapper(p1)
        val s = Json.encodeToString(PointWrapper.serializer(), pointWrapper)
        if (s != """{"value":{"x":1.0,"y":2.0}}""") return s
        val decoded = Json.decodeFromString(PointWrapper.serializer(), s)
        if (pointWrapper != decoded) return decoded.toString()
    }
    run {
        val segmentWrapper = SegmentWrapper(dSegment)
        val s = Json.encodeToString(SegmentWrapper.serializer(), segmentWrapper)
        if (s != """{"value":{"p1":{"x":1.0,"y":2.0},"p2":{"x":3.0,"y":4.0}}}""") return s
        val decoded = Json.decodeFromString(SegmentWrapper.serializer(), s)
        if (segmentWrapper != decoded) return decoded.toString()
    }
    run {
        val s = Json.encodeToString(I.serializer(), p1)
        if (s != """{"type":"DPoint","x":1.0,"y":2.0}""") return s
        val decoded = Json.decodeFromString(I.serializer(), s)
        if (p1 != decoded) return decoded.toString()
    }
    run {
        val s = Json.encodeToString(I.serializer(), dSegment)
        if (s != """{"type":"DSegment","p1":{"x":1.0,"y":2.0},"p2":{"x":3.0,"y":4.0}}""") return s
        val decoded = Json.decodeFromString(I.serializer(), s)
        if (dSegment != decoded) return decoded.toString()
    }
    return "OK"
}
