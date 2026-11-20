// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-83273
// FIR_DUMP

// FILE: Owner.kt
import lombok.Builder;

@Builder
public class Owner(val a: Int)

// FILE: test.kt
fun usage() {
    val builder = Owner.OwnerBuilder()
    builder.a(1).build()
    val justBuilder: Owner.OwnerBuilder? = null
    justBuilder?.a(2)?.build()
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localProperty, nullableType,
primaryConstructor, propertyDeclaration, safeCall */
