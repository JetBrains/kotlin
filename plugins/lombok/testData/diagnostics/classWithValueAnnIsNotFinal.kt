// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-83252

// FILE: ValueFinalDefault.java
import lombok.Value;

@Value
public class ValueFinalDefault {
    int x;
}

// FILE: TestJavaUsage.java

public class ValueDefaultChild extends ValueFinalDefault { }  // error: cannot inherit from final ValueFinalDefault

// FILE: TestKotlinUsage.kt

class KotlinChild : <!FINAL_SUPERTYPE!>ValueFinalDefault<!>(1)

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, javaType */
