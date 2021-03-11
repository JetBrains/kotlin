// "Delete redundant extension property" "true"

var Thread.<caret>priority: Int
    get() = getPriority()
    set(value) = setPriority(value)
