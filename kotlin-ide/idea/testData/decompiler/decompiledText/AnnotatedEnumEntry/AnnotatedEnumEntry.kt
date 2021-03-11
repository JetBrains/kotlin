package test

import dependency.*

enum class AnnotatedEnumEntry {
    Entry1,
    @A("2") @B(2) Entry2,
    @A("3") Entry3
}
