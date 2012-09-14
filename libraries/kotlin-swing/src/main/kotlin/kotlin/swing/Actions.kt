package kotlin.swing

import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.Icon

/**
 * Helper method to create an action from a function
 */
fun action(text: String, icon: Icon? = null, fn: (ActionEvent) -> Unit): Action {
    return object: AbstractAction(text, icon) {
        public override fun actionPerformed(e: ActionEvent?) {
            if (e != null) {
                (fn)(e)
            }
        }
    }
}