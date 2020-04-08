// "Add remaining branches with * import" "true"

package u

import e.getOwnEnum

fun mainContext() {
    val ownLocal = getOwnEnum()
    <caret>when (ownLocal) {}
}