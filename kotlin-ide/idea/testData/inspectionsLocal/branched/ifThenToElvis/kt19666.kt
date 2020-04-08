// WITH_RUNTIME
interface Taggable {
    val tag: String
}

fun Any.log() {
    val tag = <caret>if (this is Taggable) {
        tag
    }
    else {
        this::class.java.simpleName
    }
}