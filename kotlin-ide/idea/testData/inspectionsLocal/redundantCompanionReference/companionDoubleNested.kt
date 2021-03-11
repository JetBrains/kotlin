// PROBLEM: none

class Owner {
    companion object {
        class InCompanion {
            class Nested
        }
    }
}

val y = Owner.<caret>Companion.InCompanion.Nested()