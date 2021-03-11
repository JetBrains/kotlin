// "Import" "true"
// ERROR: Unresolved reference: ext

import dep.TA
import dep.ext

fun use() {
    val ta = TA()
    ta.ext<caret>
}