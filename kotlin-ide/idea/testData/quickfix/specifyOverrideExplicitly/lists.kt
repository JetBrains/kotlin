// "Specify override for 'isEmpty(): Boolean' explicitly" "true"
// WITH_RUNTIME

import java.util.*

class <caret>B(f: MutableList<String>): ArrayList<String>(), MutableList<String> by f