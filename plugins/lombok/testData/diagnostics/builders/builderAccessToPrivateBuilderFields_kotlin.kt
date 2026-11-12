// RUN_PIPELINE_TILL: FRONTEND
import lombok.Builder

@Builder
class BuilderExample(val str: String, val id: Int)

fun test() {
    val builder = BuilderExample.builder()
    builder.<!INVISIBLE_REFERENCE!>str<!> // Access to field despite the fact it's invisible
    builder.<!INVISIBLE_REFERENCE!>id<!> // Access to field despite the fact it's invisible
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, primaryConstructor, propertyDeclaration */
