package sometest

import java.util.HashSet
import java.util.HashMap
import java.util.ArrayList

val some: HashSet<Int>? = null
val some2: HashMap<Int, Int>? = null

// SET_TRUE: setCollapseImports
// REGION BEFORE: 25:94
// REGION AFTER: 25:67