// RUN_PIPELINE_TILL: CODEGEN
// RENDER_DIAGNOSTICS_FULL_TEXT
// FULL_JDK

// FILE: test.kt

import lombok.Data
import lombok.ToString

open class A(val code: Int)

// Only the `toString` warning is expected here, and it should suggest '@ToString(callSuper=false)'.
<!CALL_SUPER_NOT_CALLED!>@Data<!>
class B(code: Int, val extra: String) : A(code)

@Data
@ToString(callSuper = false)
class C(code: Int, val extra: String) : A(code)

// FILE: lombok.config

lombok.toString.callSuper=warn
lombok.equalsAndHashCode.callSuper=skip

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration */
