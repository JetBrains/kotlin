// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages

import kotlin.reflect.KProperty

class Delegate() {
    operator fun <caret>getValue(thisRef: Any?, property: KProperty<*>): String = ":)"
}

val p: String by Delegate()
