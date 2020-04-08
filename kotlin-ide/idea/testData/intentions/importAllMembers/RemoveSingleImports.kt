// INTENTION_TEXT: "Import members from 'javax.swing.SwingUtilities'"
// WITH_RUNTIME

import javax.swing.SwingUtilities
import javax.swing.SwingUtilities.invokeLater

fun foo() {
    invokeLater { }

    val bottom = <caret>SwingUtilities.BOTTOM

    SwingUtilities.invokeAndWait {
        invokeLater { }
    }

    val horizontal = javax.swing.SwingUtilities.HORIZONTAL
}
