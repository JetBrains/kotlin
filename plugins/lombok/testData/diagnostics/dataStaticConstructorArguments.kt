// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-91284
// FILE: A.java
import lombok.Data;

@Data(staticConstructor = "of")
public class A {
    private final int a;
    private final String b;
}

// FILE: test.kt
fun main() {
    val a = A(<!TOO_MANY_ARGUMENTS!>1<!>, <!TOO_MANY_ARGUMENTS!>"1"<!>)
    val b = <!INVISIBLE_REFERENCE!>A<!>()

    val c = A.of(1, "1")
    val d = A.<!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>of<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, javaFunction, javaType, localProperty, propertyDeclaration,
stringLiteral */
