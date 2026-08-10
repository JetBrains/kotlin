// ISSUE: KT-87886

import lombok.Builder
import lombok.ToString

@Builder
class PrimitivesAndNullables(
    val boolean: Boolean,
    val char: Char,
    val int: Int,
    val uint: UInt,
    val nullable: String?,
)

@Builder
class NotNullable(
    val notNullable: String,
)

@Builder
class DefaultAnn(
    @Builder.Default
    val ann: String = "default",
)

@Builder
class ChainedDefaults(
    @Builder.Default
    val a: Int = 1,
    @Builder.Default
    val b: Int = a + 1,
)

@Builder
class DefaultDependsOnPlain(
    val base: Int,
    @Builder.Default
    val derived: Int = base * 2,
)

fun box(): String {
    // Mirrors Lombok behavior with default initialization
    val obj = PrimitivesAndNullables.builder().build()
    assertEquals(false, obj.boolean)
    assertEquals('\u0000', obj.char)
    assertEquals(0, obj.int)
    assertEquals(0u, obj.uint)
    assertEquals(null, obj.nullable)

    // The default value for a non-nullable property is unclear; Lombok throws an NPE at runtime
    var npe: Boolean = false
    try {
        NotNullable.builder().build()
    } catch (e: NullPointerException) {
        npe = true
    }
    assertEquals(true, npe)

    val defaultAnnObj = DefaultAnn.builder().build()
    assertEquals("default", defaultAnnObj.ann)

    // Both defaulted: `b`'s default reads the resolved (defaulted) value of `a`.
    val bothDefaulted = ChainedDefaults.builder().build()
    assertEquals(1, bothDefaulted.a)
    assertEquals(2, bothDefaulted.b)

    // Only `a` set explicitly: `b`'s default must use the *resolved* `a`, not its own default.
    val onlyASet = ChainedDefaults.builder().a(5).build()
    assertEquals(5, onlyASet.a)
    assertEquals(6, onlyASet.b)

    // Only `b` set explicitly: `a` keeps its own default.
    val onlyBSet = ChainedDefaults.builder().b(10).build()
    assertEquals(1, onlyBSet.a)
    assertEquals(10, onlyBSet.b)

    // Both set explicitly: neither default is used.
    val bothSet = ChainedDefaults.builder().a(3).b(4).build()
    assertEquals(3, bothSet.a)
    assertEquals(4, bothSet.b)

    // A default may also depend on a plain (non-defaulted) constructor parameter.
    val dependsOnPlain = DefaultDependsOnPlain.builder().base(5).build()
    assertEquals(5, dependsOnPlain.base)
    assertEquals(10, dependsOnPlain.derived)

    return "OK"
}
