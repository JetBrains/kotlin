// PROBLEM: none

sealed class SC {
    <caret>class U : SC() {
        override fun equals(other: Any?): Boolean {
            return this === other
        }
    }
}