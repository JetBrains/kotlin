// RUN_PIPELINE_TILL: FRONTEND
// FILE: Sam.java
public interface Sam {
    void run(String a);
}

// FILE: test.kt
fun test() {
    Sam { a ->
        System.out.println(a)
    }

    Sam {
        val a = <!NO_THIS!>this<!>
        System.out.println(a)
    }
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaProperty, javaType, lambdaLiteral,
localProperty, propertyDeclaration, thisExpression */
