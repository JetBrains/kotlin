package kotlin.swing

import javax.swing.*
import java.awt.event.*
import java.awt.*

// TODO this file may be deleted when this issue is implemented
// http://youtrack.jetbrains.com/issue/KT-1752

/**
 * Creates a [[ActionListener]] for the given function for processing each [[ActionEvent]]
 */
inline fun actionListener(fn: (ActionEvent) -> Unit): ActionListener = FunctionActionListener(fn)

private class FunctionActionListener(val fn: (ActionEvent) -> Unit) : ActionListener {
    public override fun actionPerformed(e: ActionEvent?) {
        if (e != null) {
            (fn)(e)
        }
    }
}

inline fun Button.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

inline fun List.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

inline fun MenuItem.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

inline fun TextField.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

inline fun TrayIcon.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

inline fun AbstractButton.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

inline fun ButtonModel.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

inline fun ComboBoxEditor.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

inline fun DefaultButtonModel.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

/*
TODO depending on the JDK this may or may not compile with or without the type argument
inline fun JComboBox<*>.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}
*/

inline fun JFileChooser.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

inline fun JTextField.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}

inline fun Timer.addActionListener(fn: (ActionEvent) -> Unit): Unit {
    addActionListener(actionListener(fn))
}
