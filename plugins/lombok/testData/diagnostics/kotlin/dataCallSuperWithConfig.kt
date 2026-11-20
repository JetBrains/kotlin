// RUN_PIPELINE_TILL: CODEGEN
// RENDER_DIAGNOSTICS_FULL_TEXT
// FULL_JDK

// FILE: test.kt

import lombok.Data

open class A(val code: Int)

// Both implied annotations warn here: `equals/hashCode` through the default `lombok.equalsAndHashCode.callSuper=warn`,
// `toString` through the configured `lombok.toString.callSuper=warn`.
// The `toString` warning should suggest '@ToString(callSuper=false)', not '@EqualsAndHashCode(callSuper=false)'.
<!CALL_SUPER_NOT_CALLED, CALL_SUPER_NOT_CALLED!>@Data<!>
class B(code: Int, val extra: String) : A(code)

// FILE: lombok.config

lombok.toString.callSuper=warn

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration */
