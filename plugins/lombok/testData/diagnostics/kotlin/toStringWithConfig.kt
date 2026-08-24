// FULL_JDK

// FILE: test.kt

import lombok.ToString

open class Base(val baseProp: Int)

// TO_STRING_CALL_SUPER_NOT_CALLED warning: class has a non-trivial superclass and callSuper was not explicitly set
<!CALL_SUPER_NOT_CALLED!>@ToString<!>
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

// The `@Exclude` is redundant through the config alone: `lombok.toString.onlyExplicitlyIncluded` is what the
// annotation argument falls back to, so it leaves a property out whether or not `@Exclude` says so, KT-88655.
@ToString
class ExcludedUnderConfiguredOnlyExplicitlyIncluded(
    <!EXCLUDE_IS_REDUNDANT_FOR_ONLY_EXPLICITLY_INCLUDED!>@ToString.Exclude<!> val excluded: String,
    @ToString.Include val included: String,
)

// No warning: the argument outranks the config, and `false` puts every property back in.
@ToString(onlyExplicitlyIncluded = false)
class ExcludedWithArgumentOverridingConfig(@ToString.Exclude val excluded: String, val included: String)

// FILE: lombok.config

lombok.toString.callSuper=warn
lombok.toString.doNotUseGetters=true
lombok.toString.onlyExplicitlyIncluded=true
