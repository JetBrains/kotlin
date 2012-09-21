package kotlin.swing

import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.Action.*
import javax.swing.Icon

/**
 * Helper method to create an action from a function
 */
fun action(text: String, description: String? = null, mnemonic: Int? = null, icon: Icon? = null, fn: (ActionEvent) -> Unit): Action {
    val answer = object: AbstractAction(text, icon) {
        public override fun actionPerformed(e: ActionEvent?) {
            val event: ActionEvent = if (e != null) {
                e
            } else {
                // lets create a dummy event
                ActionEvent(this, ActionEvent.ACTION_PERFORMED, text)
            }
            (fn)(event)
        }
    }
    if (description != null) answer.putValue(SHORT_DESCRIPTION, description)
    if (mnemonic != null) answer.putValue(MNEMONIC_KEY, mnemonic)
    return answer
}