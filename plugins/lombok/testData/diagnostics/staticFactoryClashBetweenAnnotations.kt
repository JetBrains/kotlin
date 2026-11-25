// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

// FILE: AllArgsAndRequiredArgs.java
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

// All the fields are final, so both annotations ask for the same `of(String)` factory
@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "of")
public class AllArgsAndRequiredArgs {
    private final String name;
}

// FILE: NoArgsAndRequiredArgs.java
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

// No field is required, so both annotations ask for the same `of()` factory
@NoArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "of")
public class NoArgsAndRequiredArgs {
    private String name;
}

// FILE: AllArgsAndRequiredArgsDifferent.java
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

// Signatures match, but names are different
@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "create")
public class AllArgsAndRequiredArgsDifferent {
    private final String name;
}

// FILE: NoArgsAndRequiredArgsDifferent.java
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

// Signatures match, but names are different
@NoArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "create")
public class NoArgsAndRequiredArgsDifferent {
    private String name;
}

// FILE: test.kt
import lombok.NoArgsConstructor
import lombok.RequiredArgsConstructor

// The primary constructor has no parameters, so both annotations ask for the same `of()` factory in the companion object
<!NO_ARGS_CONSTRUCTOR_ALREADY_EXISTS!>@NoArgsConstructor(staticName = "of")<!>
<!ANNOTATION_IS_NOT_SUPPORTED!>@RequiredArgsConstructor(staticName = "of")<!>
class KotlinNoArgsAndRequiredArgs()

fun main() {
    AllArgsAndRequiredArgs.of("name")
    NoArgsAndRequiredArgs.of()
    AllArgsAndRequiredArgsDifferent.of("name")
    AllArgsAndRequiredArgsDifferent.create("name")
    NoArgsAndRequiredArgsDifferent.of()
    NoArgsAndRequiredArgsDifferent.create()
    KotlinNoArgsAndRequiredArgs.of()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, javaFunction, javaType, primaryConstructor, stringLiteral */
