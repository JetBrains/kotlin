// "Delete redundant extension property" "false"
// ACTION: Create test
// ACTION: Remove explicit type specification

var Thread.<caret>priority: Int
  get() = getPriority() + 1
  set(value) {
      setPriority(value)
  }
