// RUN_PIPELINE_TILL: FRONTEND

// FILE: AllArgsAndRequiredArgs.java
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

// All the fields are final, so both annotations ask for the same public `(String)` constructor
@AllArgsConstructor
@RequiredArgsConstructor
public class AllArgsAndRequiredArgs {
    private final String name;
}

// FILE: NoArgsAndRequiredArgs.java
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

// No field is required, so both annotations ask for the same public `()` constructor
@NoArgsConstructor
@RequiredArgsConstructor
public class NoArgsAndRequiredArgs {
    private String name;
}

// FILE: AllArgsAndNoArgs.java
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

// No fields at all, so both annotations ask for the same public `()` constructor
@AllArgsConstructor
@NoArgsConstructor
public class AllArgsAndNoArgs {
}

// FILE: TwoFactories.java
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

// Both factories are kept, but both ask for the same private `(String)` constructor behind them
@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor(staticName = "create")
public class TwoFactories {
    private final String name;
}

// FILE: FactoryAndPublicConstructor.java
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

// The private `(String)` constructor behind `of` is generated first,
// so the public `(String)` constructor of `@RequiredArgsConstructor` clashes with it and is not generated
@AllArgsConstructor(staticName = "of")
@RequiredArgsConstructor
public class FactoryAndPublicConstructor {
    private final String name;
}

// FILE: test.kt
fun main() {
    AllArgsAndRequiredArgs("name")
    NoArgsAndRequiredArgs()
    AllArgsAndNoArgs()

    <!INVISIBLE_REFERENCE!>TwoFactories<!>("name")
    TwoFactories.of("name")
    TwoFactories.create("name")

    <!INVISIBLE_REFERENCE!>FactoryAndPublicConstructor<!>("name")
    FactoryAndPublicConstructor.of("name")
}

/* GENERATED_FIR_TAGS: functionDeclaration, javaFunction, javaType, stringLiteral */
