// RUN_PIPELINE_TILL: FRONTEND
import org.jetbrains.kotlin.plugin.sandbox.MyInlineable

fun incorrect(block: <!AMBIGUOUS_FUNCTION_TYPE_KIND!>@MyInlineable suspend () -> Unit<!>) {}

/* GENERATED_FIR_TAGS: functionDeclaration */
