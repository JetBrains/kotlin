// IGNORE: see KotlinFindUsagesHandlerFactory: it is ambiguous case: ImportAlias does not have any reference to be resolved
// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtImportAlias
// OPTIONS: usages

package c

import c.a
import c.a as b<caret>

fun a() = Unit
fun a(i: Int) = Unit

fun test() {
    a()
    a(1)
    b()
    b(1)
}

// FIR_COMPARISON