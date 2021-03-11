// "Implement members" "true"
// DISABLE-ERRORS

actual interface ExpInterface {
    actual fun first()
}

actual class ExpImpl<caret> : ExpInterface