// RUN_PIPELINE_TILL: CODEGEN
// ISSUE: KT-91266
// RENDER_DIAGNOSTICS_FULL_TEXT

import lombok.Data

open class A(val code: Int)

<!CALL_SUPER_NOT_CALLED!>@Data<!>
class B(code: Int, val extra: String) : A(code)

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration */
