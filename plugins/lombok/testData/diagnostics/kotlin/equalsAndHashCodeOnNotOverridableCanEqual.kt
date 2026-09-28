// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-89189

// FILE: JavaFinalCanEqual.java

public class JavaFinalCanEqual {
    protected final boolean canEqual(Object other) {
        return other instanceof JavaFinalCanEqual;
    }
}

// FILE: JavaPrivateCanEqual.java

public class JavaPrivateCanEqual {
    private boolean canEqual(Object other) {
        return other instanceof JavaPrivateCanEqual;
    }
}

// FILE: test.kt

import lombok.EqualsAndHashCode

// The generated `canEqual` would override a final one and fail verification with "overrides final method".
open class FinalCanEqual {
    fun canEqual(other: Any?): Boolean = other is FinalCanEqual
}

<!CAN_EQUAL_FUNCTION_IS_NOT_OVERRIDABLE_IN_SUPERCLASS!>@EqualsAndHashCode(callSuper = true)<!>
class ChildOfFinalCanEqual(val a: Int) : FinalCanEqual()

open class IntermediateOfFinalCanEqual : FinalCanEqual()

<!CAN_EQUAL_FUNCTION_IS_NOT_OVERRIDABLE_IN_SUPERCLASS!>@EqualsAndHashCode(callSuper = true)<!>
class GrandChildOfFinalCanEqual(val a: Int) : IntermediateOfFinalCanEqual()

<!CAN_EQUAL_FUNCTION_IS_NOT_OVERRIDABLE_IN_SUPERCLASS!>@EqualsAndHashCode(callSuper = false)<!>
class ChildOfJavaFinalCanEqual(val a: Int) : JavaFinalCanEqual()

// A `canEqual` taking `Any` or `T` has the JVM signature of the generated one, which cannot override it.
open class NonNullCanEqual {
    open fun canEqual(other: Any): Boolean = other is NonNullCanEqual
}

<!CAN_EQUAL_FUNCTION_IS_NOT_OVERRIDABLE_IN_SUPERCLASS!>@EqualsAndHashCode(callSuper = false)<!>
class ChildOfNonNullCanEqual(val a: Int) : NonNullCanEqual()

open class TypeParameterCanEqual<T> {
    open fun canEqual(other: T): Boolean = other != null
}

<!CAN_EQUAL_FUNCTION_IS_NOT_OVERRIDABLE_IN_SUPERCLASS!>@EqualsAndHashCode(callSuper = false)<!>
class ChildOfTypeParameterCanEqual(val a: Int) : TypeParameterCanEqual<String>()

// No error: an open `canEqual(other: Any?)` is overridden by the generated one.
open class OpenCanEqual {
    open fun canEqual(other: Any?): Boolean = other is OpenCanEqual
}

@EqualsAndHashCode(callSuper = false)
class ChildOfOpenCanEqual(val a: Int) : OpenCanEqual()

// No error: a private `canEqual` is not inherited, so the generated one does not override it.
open class PrivateCanEqual {
    private fun canEqual(other: Any?): Boolean = other is PrivateCanEqual
}

@EqualsAndHashCode(callSuper = false)
class ChildOfPrivateCanEqual(val a: Int) : PrivateCanEqual()

@EqualsAndHashCode(callSuper = false)
class ChildOfJavaPrivateCanEqual(val a: Int) : JavaPrivateCanEqual()

// No error: an overload with another JVM signature is unrelated.
open class FinalUnrelatedCanEqual {
    fun canEqual(other: Int): Boolean = other == 0
}

@EqualsAndHashCode(callSuper = false)
class ChildOfFinalUnrelatedCanEqual(val a: Int) : FinalUnrelatedCanEqual()

// No error: nothing is generated over the user's own `canEqual`, so the final override is reported by the platform.
@EqualsAndHashCode(callSuper = false)
class ChildOfFinalCanEqualWithOwnCanEqual(val a: Int) : FinalCanEqual() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun canEqual(other: Any?): Boolean = other is ChildOfFinalCanEqualWithOwnCanEqual
}
