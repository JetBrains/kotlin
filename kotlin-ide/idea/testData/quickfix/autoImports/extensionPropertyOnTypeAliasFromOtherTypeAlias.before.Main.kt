// "Import" "true"
// ERROR: Unresolved reference: ext

import dep.TA1

fun use() {
    val ta = TA1()
    ta.ext<caret>
}