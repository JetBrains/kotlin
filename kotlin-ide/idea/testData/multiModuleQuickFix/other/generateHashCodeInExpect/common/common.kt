// "Generate 'hashCode()'" "true"
// TOOL: org.jetbrains.kotlin.idea.inspections.EqualsOrHashCodeInspection

expect class With<caret>Constructor(x: Int, s: String) {
    val x: Int
    val s: String

    override fun equals(other: Any?): Boolean
}