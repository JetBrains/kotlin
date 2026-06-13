// FULL_JDK

// FILE: test.kt

import lombok.ToString

open class Base(val baseProp: Int)

// TO_STRING_CALL_SUPER_NOT_CALLED warning: class has a non-trivial superclass and callSuper was not explicitly set
<!TO_STRING_CALL_SUPER_NOT_CALLED!>@ToString<!>
class DerivedImplicit(val ownProp: String) : Base(10)

// No TO_STRING_CALL_SUPER_NOT_CALLED warning: class extends only kotlin.Any
@ToString
class Simple(val x: Int)

// No TO_STRING_CALL_SUPER_NOT_CALLED warning: callSuper explicitly set to true in annotation (overrides config)
@ToString(callSuper = true)
class DerivedCallSuperTrue(val ownProp: String) : Base(10)

// No TO_STRING_CALL_SUPER_NOT_CALLED warning: callSuper explicitly set to false in annotation (overrides config)
@ToString(callSuper = false)
class DerivedCallSuperFalse(val ownProp: String) : Base(10)

// FILE: lombok.config

lombok.toString.callSuper=warn
lombok.toString.doNotUseGetters=true
