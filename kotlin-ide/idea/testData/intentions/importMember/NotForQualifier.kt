// IS_APPLICABLE: false
// WITH_RUNTIME

import javax.swing.SwingUtilities

fun foo() {
    <caret>SwingUtilities.invokeLater {
    }
}
