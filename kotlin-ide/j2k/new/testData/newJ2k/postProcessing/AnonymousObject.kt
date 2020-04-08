import javax.swing.SwingUtilities

class A {
    fun foo() {
        SwingUtilities.invokeLater { println("a") }
    }
}