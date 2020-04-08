// PROBLEM: none

import Owner.<caret>Companion.some

class Owner {
    companion object {
        const val some = ""
    }
}

class User {
    val anything = some
}