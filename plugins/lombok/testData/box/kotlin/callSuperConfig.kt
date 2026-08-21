// ISSUE: KT-88771
// DUMP_KT_IR
// FULL_JDK

// FILE: test.kt

import lombok.EqualsAndHashCode
import lombok.ToString

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
    if (implicit != DerivedImplicit("same", 1)) return "FAIL: chained equals rejects an equal instance"
    if (implicit == DerivedImplicit("same", 2)) return "FAIL: equals did not chain to Base"
    if (implicit.hashCode() != DerivedImplicit("same", 1).hashCode()) return "FAIL: chained hashCode is unstable"
    if (implicit.hashCode() == DerivedImplicit("same", 2).hashCode()) return "FAIL: hashCode did not chain to Base"
    if (implicit.toString() != "DerivedImplicit(super=Base(baseProp=1), ownProp=same)") return "FAIL: $implicit"

    val explicitFalse = DerivedExplicitFalse("same", 1)
    if (explicitFalse != DerivedExplicitFalse("same", 2)) return "FAIL: equals chained despite callSuper=false"
    if (explicitFalse.hashCode() != DerivedExplicitFalse("same", 2).hashCode()) {
        return "FAIL: hashCode chained despite callSuper=false"
    }
    if (explicitFalse.toString() != "DerivedExplicitFalse(ownProp=same)") return "FAIL: $explicitFalse"

    val explicitTrue = DerivedExplicitTrue("same", 1)
    if (explicitTrue == DerivedExplicitTrue("same", 2)) return "FAIL: equals did not chain despite callSuper=true"
    if (explicitTrue.toString() != "DerivedExplicitTrue(super=Base(baseProp=1), ownProp=same)") {
        return "FAIL: $explicitTrue"
    }

    val notDerived = NotDerivedImplicit("same")
    if (notDerived != NotDerivedImplicit("same")) return "FAIL: equals chained to Any"
    if (notDerived.hashCode() != NotDerivedImplicit("same").hashCode()) return "FAIL: hashCode chained to Any"
    if (notDerived.toString() != "NotDerivedImplicit(ownProp=same)") return "FAIL: $notDerived"

    return "OK"
}

// FILE: lombok.config

lombok.equalsAndHashCode.callSuper=call
lombok.toString.callSuper=call
