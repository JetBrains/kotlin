// "Import" "true"
// ERROR: Unresolved reference: ext

import dep.TTA

fun use(taa: TTA) {
    taa.ext<caret>()
}