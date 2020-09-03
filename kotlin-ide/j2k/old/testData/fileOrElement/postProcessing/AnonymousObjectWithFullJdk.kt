import javax.swing.*

class A {
    internal fun foo() {
        SwingUtilities.invokeLater { println("a") }
    }
}
