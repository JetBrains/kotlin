package kotlin.swing

import kotlin.beans.*

import javax.swing.*
import java.awt.event.*
import java.awt.*
import java.beans.*

// TODO this file may be deleted when this issue is implemented
// http://youtrack.jetbrains.com/issue/KT-1752

inline fun Component.addPropertyChangeListener(fn: (PropertyChangeEvent) -> Unit): Unit {
    addPropertyChangeListener(propertyChangeListener(fn))
}

inline fun Component.addPropertyChangeListener(name: String, fn: (PropertyChangeEvent) -> Unit): Unit {
    addPropertyChangeListener(name, propertyChangeListener(fn))
}
