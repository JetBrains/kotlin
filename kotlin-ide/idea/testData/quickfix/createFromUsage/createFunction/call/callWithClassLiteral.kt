// "Create function 'checkProperty'" "true"
internal object model {
    val layer = ""
}

fun main(args: Array<String>) {
    <caret>checkProperty(model.layer::class)
}