// KT-2424 Invoking completion adds unnecessary FQ name

fun main(args: Array<String>) {
    throw IllegalAccessExceptio<caret> //Press Ctrl+Space and select it
}