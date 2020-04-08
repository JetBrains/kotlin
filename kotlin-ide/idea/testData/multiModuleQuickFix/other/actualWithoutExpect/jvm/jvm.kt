// "Remove 'actual' modifier" "true"

actual interface ExpInterface {
    actual fun first()
}

actual class ExpImpl : ExpInterface {
    actual override fun <caret>first() { }
}