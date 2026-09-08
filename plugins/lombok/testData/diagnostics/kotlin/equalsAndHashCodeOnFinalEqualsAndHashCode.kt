// ISSUE: KT-88511

// FILE: JavaFinalEquals.java

public class JavaFinalEquals {
    @Override
    public final boolean equals(Object other) {
        return this == other;
    }
}

// FILE: JavaFinalHashCode.java

public class JavaFinalHashCode {
    @Override
    public final int hashCode() {
        return 0;
    }
}

// FILE: test.kt

import lombok.EqualsAndHashCode

// Only `hashCode` is final, which is enough: `@EqualsAndHashCode` always generates both, and the class then
// fails verification with "overrides final method" instead of failing to compile.
open class FinalHashCode {
    final override fun hashCode(): Int = 0
}

<!CALL_SUPER_NOT_CALLED, EQUALS_OR_HASH_CODE_FUNCTIONS_ARE_FINAL_IN_SUPERCLASS!>@EqualsAndHashCode<!>
class ChildOfFinalHashCode : FinalHashCode() {
    val a = 1
}

open class FinalEquals {
    final override fun equals(other: Any?): Boolean = this === other
}

<!CALL_SUPER_NOT_CALLED, EQUALS_OR_HASH_CODE_FUNCTIONS_ARE_FINAL_IN_SUPERCLASS!>@EqualsAndHashCode<!>
class ChildOfFinalEquals : FinalEquals() {
    val a = 1
}

open class IntermediateOfFinalHashCode : FinalHashCode()

<!CALL_SUPER_NOT_CALLED, EQUALS_OR_HASH_CODE_FUNCTIONS_ARE_FINAL_IN_SUPERCLASS!>@EqualsAndHashCode<!>
class GrandChildOfFinalHashCode : IntermediateOfFinalHashCode() {
    val a = 1
}

<!CALL_SUPER_NOT_CALLED, EQUALS_OR_HASH_CODE_FUNCTIONS_ARE_FINAL_IN_SUPERCLASS!>@EqualsAndHashCode<!>
class ChildOfJavaFinalEquals : JavaFinalEquals() {
    val a = 1
}

<!CALL_SUPER_NOT_CALLED, EQUALS_OR_HASH_CODE_FUNCTIONS_ARE_FINAL_IN_SUPERCLASS!>@EqualsAndHashCode<!>
class ChildOfJavaFinalHashCode : JavaFinalHashCode() {
    val a = 1
}

// No error: the inherited members are open, so the generated ones may override them
open class OpenEqualsAndHashCode {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = 0
}

<!CALL_SUPER_NOT_CALLED!>@EqualsAndHashCode<!>
class ChildOfOpenEqualsAndHashCode : OpenEqualsAndHashCode() {
    val a = 1
}

// No error: a final member with a different signature is not one of the generated ones
open class FinalUnrelatedMembers {
    fun equals(other: FinalUnrelatedMembers): Boolean = this === other
    fun hashCode(salt: Int): Int = salt
}

<!CALL_SUPER_NOT_CALLED!>@EqualsAndHashCode<!>
class ChildOfFinalUnrelatedMembers : FinalUnrelatedMembers() {
    val a = 1
}

// Nothing is generated at all, so the final override is reported by the platform, not by Lombok
<!CALL_SUPER_NOT_CALLED, EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST!>@EqualsAndHashCode<!>
class ChildOfFinalHashCodeWithOwnHashCode : FinalHashCode() {
    <!OVERRIDING_FINAL_MEMBER!>override<!> fun hashCode(): Int = 1
    val a = 1
}
