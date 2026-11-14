// RUN_PIPELINE_TILL: FRONTEND
// FILE: Sam.java
@SamWithReceiver
public interface Sam {
    void run(String a);
}

// FILE: test.kt
annotation class SamWithReceiver

fun test() {
    Sam <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE!>a<!> ->
        System.out.println(a)
    }<!>

    Sam {
        val a: String = this
        val a2: String = <!UNRESOLVED_REFERENCE!>it<!>
        System.out.println(a)
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, flexibleType, functionDeclaration, javaFunction, javaProperty, javaType,
lambdaLiteral, localProperty, propertyDeclaration, thisExpression */
