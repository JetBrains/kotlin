// RUN_PIPELINE_TILL: FRONTEND

// FILE: Sam.java
@SamWithReceiver
public interface Sam {
    void run();
}

// FILE: Exec.java
public class Exec {
    void exec(Sam sam) {}
}

// FILE: test.kt
annotation class SamWithReceiver

fun test() {
    val e = Exec()

    e.exec {
        System.out.println("Hello, world!")
    }

    e.exec {
        val a: String = <!NO_THIS!>this<!>
        System.out.println(a)
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, flexibleType, functionDeclaration, javaFunction, javaProperty, javaType,
lambdaLiteral, localProperty, propertyDeclaration, samConversion, stringLiteral, thisExpression */
