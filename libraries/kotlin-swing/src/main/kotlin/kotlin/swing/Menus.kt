package kotlin.swing

import javax.swing.*

public fun JMenuBar.menu(text: String, init: JMenu.() -> Unit): JMenu {
    val answer = JMenu(text)
    answer.init()
    add(answer)
    return answer;
}

public fun JPopupMenu.menu(text: String, init: JMenu.() -> Unit): JMenu {
    val answer = JMenu(text)
    answer.init()
    add(answer)
    return answer;
}

var JMenu.mnemonic: Int
get() {
    return getMnemonic()
}
set(value) {
    setMnemonic(value)
}

var JMenu.accelerator: KeyStroke?
get() {
    return getAccelerator()
}
set(value) {
    setAccelerator(value)
}

fun JMenu.add(vararg actions: Action): Unit {
    for (action in actions) {
        val answer = JMenuItem(action)
        add(answer)
    }
}

fun JMenu.item(text: String, description: String? = null, mnemonic: Char? = null, accelerator: KeyStroke? = null): JMenuItem {
    val answer = menuItem(text, description, mnemonic, accelerator)
    add(answer)
    return answer
}

fun JMenu.checkBoxItem(text: String, description: String? = null, mnemonic: Char? = null, accelerator: KeyStroke? = null): JCheckBoxMenuItem {
    val answer = checkBoxMenuItem(text, description, mnemonic, accelerator)
    add(answer)
    return answer
}

fun JMenu.radioButtonItem(text: String, description: String? = null, mnemonic: Char? = null, accelerator: KeyStroke? = null): JRadioButtonMenuItem {
    val answer = radioButtonMenuItem(text, description, mnemonic, accelerator)
    add(answer)
    return answer
}

/**
 * Helper method to create a new [JMenuItem]
 */
public fun menuItem(text: String, description: String? = null, mnemonic: Char? = null, accelerator: KeyStroke? = null): JMenuItem {
    return configureMenuItem(JMenuItem(text), description, mnemonic, accelerator)
}

/**
 * Helper method to create a new [JCheckBoxMenuItem]
 */
public fun checkBoxMenuItem(text: String, description: String? = null, mnemonic: Char? = null, accelerator: KeyStroke? = null): JCheckBoxMenuItem {
    return configureMenuItem(JCheckBoxMenuItem(text), description, mnemonic, accelerator)
}

/**
 * Helper method to create a new [JRadioButtonMenuItem]
 */
public fun radioButtonMenuItem(text: String, description: String? = null, mnemonic: Char? = null, accelerator: KeyStroke? = null): JRadioButtonMenuItem {
    return configureMenuItem(JRadioButtonMenuItem(text), description, mnemonic, accelerator)
}

/**
 * Helper method to create a new [JMenuItem]
 */
fun <T: JMenuItem> configureMenuItem(answer: T, description: String?, mnemonic: Char?, accelerator: KeyStroke?): T {
    if (description != null) {
        answer.getAccessibleContext()?.setAccessibleDescription(description)
    }
    if (mnemonic != null) {
        answer.setMnemonic(mnemonic)
    }
    if (accelerator != null) {
        answer.setAccelerator(accelerator)
    }
    return answer
}

/**
 * Helper function to create a [KeyStroke]
 */
public fun keyStroke(keyChar: Char, modifiers: Int?): KeyStroke? {
    return if (modifiers != null) {
        KeyStroke.getKeyStroke(keyChar, modifiers)
    } else {
        KeyStroke.getKeyStroke(keyChar)
    }
}

/**
 * Creates a [JMenuBar] from a list of [JMenu] objects
 */
public fun menuBar(init: JMenuBar.() -> Unit): JMenuBar {
    val answer = JMenuBar()
    answer.init()
    return answer
}

/**
 * Creates a [JPopupMenu] from a list of [JMenu] objects
 */
public fun popupMenu(init: JPopupMenu.() -> Unit): JPopupMenu {
    val answer = JPopupMenu()
    answer.init()
    return answer
}
