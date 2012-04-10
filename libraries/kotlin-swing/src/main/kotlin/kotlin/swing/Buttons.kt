package kotlin.swing

import javax.swing.*
import java.awt.event.*
import java.awt.*

fun button(text: String, action: (ActionEvent) -> Unit): JButton {
    val result = JButton(text)
    result.addActionListener(action)
    return result
}
