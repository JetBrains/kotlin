// RUNTIME_WITH_FULL_JDK

import javax.swing.*;

public class A {
    void foo() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("a");
            }
        });
    }
}
