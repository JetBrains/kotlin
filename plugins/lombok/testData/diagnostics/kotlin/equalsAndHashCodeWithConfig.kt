// RENDER_DIAGNOSTICS_FULL_TEXT
// FULL_JDK

// FILE: test.kt

import lombok.EqualsAndHashCode

open class Base(val baseProp: Int)

// EQUALS_AND_HASH_CODE_CALL_SUPER_NOT_CALLED warning: class has a non-trivial superclass and callSuper was not explicitly set
<!CALL_SUPER_NOT_CALLED!>@EqualsAndHashCode<!>
class DerivedImplicit(val ownProp: String) : Base(10)

// No warning: class extends only kotlin.Any
@EqualsAndHashCode
class Simple(val x: Int)

// No warning: callSuper explicitly set to true (overrides config)
@EqualsAndHashCode(callSuper = true)
class DerivedCallSuperTrue(val ownProp: String) : Base(10)

// No warning: callSuper explicitly set to false (overrides config)
@EqualsAndHashCode(callSuper = false)
class DerivedCallSuperFalse(val ownProp: String) : Base(10)

// FILE: lombok.config

lombok.equalsAndHashCode.callSuper=warn
lombok.equalsAndHashCode.doNotUseGetters=true
