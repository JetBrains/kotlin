// ISSUE: KT-88653
// RENDER_DIAGNOSTICS_FULL_TEXT

// The point of this file is the *absence* of a `lombok.config`: `lombok.equalsAndHashCode.callSuper` defaults
// to `warn`, unlike `lombok.toString.callSuper`, which defaults to `skip` (see `toString.kt`).

import lombok.EqualsAndHashCode

open class Base(val baseProp: Int)

// `callSuper` was not set and the class extends something, so the generated pair silently ignores `Base`.
<!CALL_SUPER_NOT_CALLED!>@EqualsAndHashCode<!>
class DerivedImplicit(val ownProp: String) : Base(10)

// No warning: `callSuper` is explicit, whichever way it points, so there is nothing to point out.
@EqualsAndHashCode(callSuper = true)
class DerivedCallSuperTrue(val ownProp: String) : Base(10)

@EqualsAndHashCode(callSuper = false)
class DerivedCallSuperFalse(val ownProp: String) : Base(10)

// `Any.equals` compares by identity, so chaining to it would make the generated pair reject everything.
@EqualsAndHashCode(callSuper = <!CALL_SUPER_TO_ANY_IS_POINTLESS!>true<!>)
class NotDerivedCallSuperTrue(val ownProp: String)

// No diagnostic: `Any` is the only superclass either way, and neither the annotation nor the config claims
// otherwise.
@EqualsAndHashCode
class NotDerivedImplicit(val ownProp: String)

@EqualsAndHashCode(callSuper = false)
class NotDerivedCallSuperFalse(val ownProp: String)

// Neither `callSuper` diagnostic applies where nothing is generated in the first place: an enum does extend
// `Enum`, and an object does extend nothing but `Any`, yet neither gets an `equals`/`hashCode` to chain.
<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!>
enum class EnumImplicit { ENTRY }

<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode(callSuper = true)<!>
object ObjectCallSuperTrue
