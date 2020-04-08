// IS_APPLICABLE: false
// WITH_RUNTIME
// ERROR: Unresolved reference: xxx

import javax.swing.SwingUtilities

fun foo() {
    SwingUtilities.<caret>xxx {
    }
}
