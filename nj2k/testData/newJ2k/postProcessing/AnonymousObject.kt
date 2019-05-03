import javax.swing.SwingUtilities

class A {
    internal fun foo() {
        SwingUtilities.invokeLater { println("a") }
    }
}
