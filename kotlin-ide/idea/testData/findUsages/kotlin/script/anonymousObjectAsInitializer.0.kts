// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtObjectDeclaration
// OPTIONS: usages
val x = <caret>object: Any() {

}

// ERROR: No script runtime was found in the classpath: class 'kotlin.script.templates.standard.ScriptTemplateWithArgs' not found. Please add kotlin-script-runtime.jar to the module dependencies.