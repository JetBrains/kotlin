// RUNTIME_WITH_FULL_JDK
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Enumeration;

public class TestJavaExpectedTypeInference {
    public void test(DefaultMutableTreeNode node) {
        for (Enumeration<DefaultMutableTreeNode> e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode child = e.nextElement();
            String name = (String) child.getUserObject();
            System.out.println(name);
        }
    }
}