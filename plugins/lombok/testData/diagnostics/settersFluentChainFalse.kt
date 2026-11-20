// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-90387
// FILE: ChainFalseProbe.java

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true, chain = false)
public class ChainFalseProbe {
    private String name;
}

// FILE: FluentOnlyProbe.java

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class FluentOnlyProbe {
    private String name;
}

// FILE: ProbeCalled.kt

fun callChainFalse(): String? {
    val probe = ChainFalseProbe()
    probe.name("x")
    return probe.name()
}

fun callFluentOnly(): String? {
    val probe = FluentOnlyProbe()
    probe.name("x")
    return probe.name()
}

fun compileTimeMismatch(probe: ChainFalseProbe) {
    val result: Unit = probe.<!INITIALIZER_TYPE_MISMATCH!>name<!>("x")
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, javaFunction, javaType, localProperty, nullableType,
propertyDeclaration, stringLiteral */
