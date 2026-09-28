// ISSUE: KT-88771
// DUMP_KT_IR
// FULL_JDK

// FILE: test.kt

import lombok.EqualsAndHashCode
import lombok.ToString
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

// `Base` speaks about its own state, so a subclass that chains to it tells `baseProp` apart and one that does
// not cannot - which is what every check below turns on.
open class Base(val baseProp: Int) {
    override fun equals(other: Any?): Boolean = other is Base && other.baseProp == baseProp
    override fun hashCode(): Int = baseProp
    override fun toString(): String = "Base(baseProp=$baseProp)"
}

// `callSuper` is left to `lombok.config`, which says `call`, so all three members chain to `Base`.
@EqualsAndHashCode
@ToString
class DerivedImplicit(val ownProp: String, baseProp: Int) : Base(baseProp)

// An explicit `callSuper` outranks the config, in either direction.
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = false)
class DerivedExplicitFalse(val ownProp: String, baseProp: Int) : Base(baseProp)

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
class DerivedExplicitTrue(val ownProp: String, baseProp: Int) : Base(baseProp)

// A project-wide `call` cannot mean this class: `Any` compares and hashes by identity, so chaining to it would
// make every instance unequal to every other one.
@EqualsAndHashCode
@ToString
class NotDerivedImplicit(val ownProp: String)

fun box(): String {
    val implicit = DerivedImplicit("same", 1)
    assertEquals(DerivedImplicit("same", 1), implicit, "chained equals rejects an equal instance")
    assertNotEquals(DerivedImplicit("same", 2), implicit, "equals did not chain to Base")
    assertEquals(DerivedImplicit("same", 1).hashCode(), implicit.hashCode(), "chained hashCode is unstable")
    assertNotEquals(DerivedImplicit("same", 2).hashCode(), implicit.hashCode(), "hashCode did not chain to Base")
    assertEquals("DerivedImplicit(super=Base(baseProp=1), ownProp=same)", implicit.toString())

    val explicitFalse = DerivedExplicitFalse("same", 1)
    assertEquals(DerivedExplicitFalse("same", 2), explicitFalse, "equals chained despite callSuper=false")
    assertEquals(
        DerivedExplicitFalse("same", 2).hashCode(),
        explicitFalse.hashCode(),
        "hashCode chained despite callSuper=false"
    )
    assertEquals("DerivedExplicitFalse(ownProp=same)", explicitFalse.toString())

    val explicitTrue = DerivedExplicitTrue("same", 1)
    assertNotEquals(DerivedExplicitTrue("same", 2), explicitTrue, "equals did not chain despite callSuper=true")
    assertEquals("DerivedExplicitTrue(super=Base(baseProp=1), ownProp=same)", explicitTrue.toString())

    val notDerived = NotDerivedImplicit("same")
    assertEquals(NotDerivedImplicit("same"), notDerived, "equals chained to Any")
    assertEquals(NotDerivedImplicit("same").hashCode(), notDerived.hashCode(), "hashCode chained to Any")
    assertEquals("NotDerivedImplicit(ownProp=same)", notDerived.toString())

    return "OK"
}

// FILE: lombok.config

lombok.equalsAndHashCode.callSuper=call
lombok.toString.callSuper=call
