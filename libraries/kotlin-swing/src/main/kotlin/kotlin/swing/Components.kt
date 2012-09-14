package kotlin.swing

import javax.swing.JComponent

var JComponent.description: String?
get() {
    return getAccessibleContext()?.getAccessibleDescription()
}
set(value) {
    getAccessibleContext()?.setAccessibleDescription(value)
}