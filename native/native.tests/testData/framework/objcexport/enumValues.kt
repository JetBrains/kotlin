package enumValues

import noEnumEntries.*

enum class EnumLeftRightUpDown {
    LEFT, RIGHT, UP, DOWN
}

enum class EnumOneTwoThreeValues {
    ONE, TWO, THREE, VALUES, ENTRIES
}

enum class EnumValuesValues_ {
    VALUES, VALUES_, ENTRIES, ENTRIES_
}

enum class EmptyEnum {
}


fun dceAvoidance() : NoEnumEntriesEnum {
    return NoEnumEntriesEnum.ONE
}