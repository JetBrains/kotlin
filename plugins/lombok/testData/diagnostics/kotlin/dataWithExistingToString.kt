// RUN_PIPELINE_TILL: FRONTEND
// Lombok skips the members `@Data` implies silently when the class already declares them
// (`whineIfExists = false` in `HandleData`), so neither class annotated with `@Data` below should get a warning.

import lombok.Data
import lombok.ToString
import lombok.EqualsAndHashCode

@Data
class DataWithExistingToString(val x: Int) {
    override fun toString(): String = "custom"
}

@Data
class DataWithExistingEqualsAndHashCode(val x: Int) {
    override fun equals(other: Any?): Boolean = other is DataWithExistingEqualsAndHashCode
    override fun hashCode(): Int = 0
}

// An explicit annotation does warn, as in Lombok.
<!TO_STRING_FUNCTION_ALREADY_EXISTS!>@ToString<!>
class ToStringWithExistingToString(val x: Int) {
    override fun toString(): String = "custom"
}

<!EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST!>@EqualsAndHashCode<!>
class EqualsAndHashCodeWithExistingEqualsAndHashCode(val x: Int) {
    override fun equals(other: Any?): Boolean = other is EqualsAndHashCodeWithExistingEqualsAndHashCode
    override fun hashCode(): Int = 0
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, isExpression, nullableType, operator,
override, primaryConstructor, propertyDeclaration, stringLiteral */
