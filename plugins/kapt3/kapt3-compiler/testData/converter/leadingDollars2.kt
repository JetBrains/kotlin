// FIR_BLOCKED: LC don't support names with $
// CORRECT_ERROR_TYPES

// FILE: te/st/a/JavaClass.java
package te.st.a;

public class JavaClass {}

// FILE: test.kt
@file:Suppress("UNRESOLVED_REFERENCE")
package te.st.a

import te.st.a.`$Test`.Inner as MyInner
import te.st.a.`Test$`.Inner as MyInner2
import te.st.a.`Test$`.`Inner$` as MyInner3

fun a(a: MyInner) {}
