// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +RichErrors
annotation class AllOpen

@AllOpen error class <!NON_FINAL_RICH_ERROR_CLASS!>Foo<!>

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration */
