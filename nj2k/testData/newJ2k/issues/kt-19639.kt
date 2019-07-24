// ERROR: Type mismatch: inferred type is Enumeration<(raw) Any!>! but Enumeration<DefaultMutableTreeNode> was expected
import java.util.Enumeration
import javax.swing.tree.DefaultMutableTreeNode

class TestJavaExpectedTypeInference {
    fun test(node: DefaultMutableTreeNode) {
        val e: Enumeration<DefaultMutableTreeNode> = node.children()
        while (e.hasMoreElements()) {
            val child: DefaultMutableTreeNode = e.nextElement()
            val name = child.userObject as String
            println(name)
        }
    }
}