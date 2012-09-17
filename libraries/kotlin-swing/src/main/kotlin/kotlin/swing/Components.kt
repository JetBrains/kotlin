package kotlin.swing

import java.awt.Dimension
import java.awt.Component
import javax.swing.JComponent

var JComponent.description: String?
    get() {
        return getAccessibleContext()?.getAccessibleDescription()
    }
    set(value) {
        getAccessibleContext()?.setAccessibleDescription(value)
    }

var JComponent.preferredWidth: Int
    get() = getPreferredSize().width()
    set(value) {
        setPreferredSize(getPreferredSize().width(value))
    }
var JComponent.preferredHeight: Int
    get() = getPreferredSize().height()
    set(value) {
        setPreferredSize(getPreferredSize().height(value))
    }

var Component.minimumWidth: Int
    get() = getMinimumSize().width()
    set(value) {
        setMinimumSize(getMinimumSize().width(value))
    }
var Component.minimumHeight: Int
    get() = getMinimumSize().height()
    set(value) {
        setMinimumSize(getMinimumSize().height(value))
    }

var Component.maximumWidth: Int
    get() = getMaximumSize().width()
    set(value) {
        setMaximumSize(getMaximumSize().width(value))
    }
var Component.maximumHeight: Int
    get() = getMaximumSize().height()
    set(value) {
        setMaximumSize(getMaximumSize().height(value))
    }

var Component.width: Int
    get() = getSize().width()
    set(value) {
        setSize(getSize().width(value))
    }
var Component.height: Int
    get() = getSize().height()
    set(value) {
        setSize(getSize().height(value))
    }


