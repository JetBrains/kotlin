// FULL_JDK

// FILE: test.kt

import lombok.ToString

open class Base(val baseProp: Int)

// Warning: class has a non-trivial superclass and callSuper was not explicitly set
<!TO_STRING_CALL_SUPER_NOT_CALLED!>@ToString<!>
class DerivedImplicit(val ownProp: String) : Base(10)

// No warning: class extends only kotlin.Any
@ToString
class Simple(val x: Int)

// No warning: callSuper explicitly set to true in annotation (overrides config)
@ToString(callSuper = true)
class DerivedCallSuperTrue(val ownProp: String) : Base(10)

// No warning: callSuper explicitly set to false in annotation (overrides config)
@ToString(callSuper = false)
class DerivedCallSuperFalse(val ownProp: String) : Base(10)

// FILE: lombok.config
lombok.toString.callSuper=warn
