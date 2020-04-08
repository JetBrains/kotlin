// PSI_ELEMENT: com.intellij.psi.PsiPackage
// OPTIONS: usages
// FIND_BY_REF
package <caret>foo.bar

import foo.bar.X

class X

val x: foo.bar.X? = foo.bar.X()