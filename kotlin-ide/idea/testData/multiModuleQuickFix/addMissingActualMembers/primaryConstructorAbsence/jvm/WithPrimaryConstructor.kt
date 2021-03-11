// "Add missing actual members" "true"
// DISABLE-ERRORS

actual class <caret>WithPrimaryConstructor {
    fun bar(x: String) {}

    val z: Double = 3.14
}
