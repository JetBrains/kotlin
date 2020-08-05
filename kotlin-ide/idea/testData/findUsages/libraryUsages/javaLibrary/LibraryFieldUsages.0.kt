// PSI_ELEMENT: com.intellij.psi.PsiField
// OPTIONS: usages
// FIND_BY_REF
package usages

import library.Foo

fun test() {
    Foo(1).<caret>x = 1
}