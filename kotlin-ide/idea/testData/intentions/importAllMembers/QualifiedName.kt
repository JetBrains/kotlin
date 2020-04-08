// INTENTION_TEXT: "Import members from 'javax.swing.SwingUtilities'"
// WITH_RUNTIME

fun foo() {
    javax.swing.SwingUtilities<caret>.invokeLater { }
}
