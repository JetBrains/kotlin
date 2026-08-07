// ISSUE: KT-88420

// FILE: JavaFinalToString.java

public class JavaFinalToString {
    @Override
    public final String toString() {
        return "JavaFinalToString()";
    }
}

// FILE: test.kt

import lombok.ToString

open class FinalToString {
    final override fun toString(): String = "FinalToString()"
}

<!TO_STRING_FUNCTION_IS_FINAL_IN_SUPERCLASS!>@ToString<!>
class ChildOfFinalToString : FinalToString() {
    val a = 1
}

open class IntermediateOfFinalToString : FinalToString()

<!TO_STRING_FUNCTION_IS_FINAL_IN_SUPERCLASS!>@ToString<!>
class GrandChildOfFinalToString : IntermediateOfFinalToString() {
    val a = 1
}

<!TO_STRING_FUNCTION_IS_FINAL_IN_SUPERCLASS!>@ToString<!>
class ChildOfJavaFinalToString : JavaFinalToString() {
    val a = 1
}

// No error: the inherited 'toString()' is open, so the generated one may override it
open class OpenToString {
    override fun toString(): String = "OpenToString()"
}

@ToString
class ChildOfOpenToString : OpenToString() {
    val a = 1
}

// Nothing is generated at all, so the final override is reported by the platform, not by Lombok
<!TO_STRING_FUNCTION_ALREADY_EXISTS!>@ToString<!>
class ChildOfFinalToStringWithOwnToString : FinalToString() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun toString(): String = "own"
    val a = 1
}
