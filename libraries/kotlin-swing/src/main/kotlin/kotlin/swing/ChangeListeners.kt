package kotlin.swing

import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.colorchooser.*
import javax.swing.event.*
import javax.swing.text.*

// TODO this file may be deleted when this issue is implemented
// http://youtrack.jetbrains.com/issue/KT-1752

/**
 * Creates a [[ChangeListener]] for the given function for processing each [[ChangeEvent]]
 */
inline fun changeListener(fn: (ChangeEvent) -> Unit): ChangeListener = FunctionChangeListener(fn)

private class FunctionChangeListener(val fn: (ChangeEvent) -> Unit) : ChangeListener {
    public override fun stateChanged(e: ChangeEvent) {
        if (e != null) {
            (fn)(e)
        }
    }
}

inline fun AbstractButton.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun AbstractSpinnerModel.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun BoundedRangeModel.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun ButtonModel.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun ColorSelectionModel.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun DefaultColorSelectionModel.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun DefaultSingleSelectionModel.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun JProgressBar.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun DefaultButtonModel.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun JSlider.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun JSpinner.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun JViewport.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun MenuSelectionManager.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun SingleSelectionModel.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun SpinnerModel.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun Caret.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun DefaultCaret.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun Style.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

inline fun StyleContext.addChangeListener(fn: (ChangeEvent) -> Unit): Unit {
    addChangeListener(changeListener(fn))
}

