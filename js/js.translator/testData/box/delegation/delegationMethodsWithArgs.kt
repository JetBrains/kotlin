// EXPECTED_REACHABLE_NODES: 505
// This test was adapted from compiler/testData/codegen/box/classes
package foo

interface TextField {
    fun getText(): String
}

interface InputTextField : TextField {
    fun setText(text: String)
}

interface MooableTextField : InputTextField {
    fun moo(a: Int, b: Int, c: Int): Int
}

class SimpleTextField : MooableTextField {
    private var text2 = ""
    override fun getText() = text2
    override fun setText(text: String) {
        this.text2 = text
    }
    override fun moo(a: Int, b: Int, c: Int) = a + b + c
}

class TextFieldWrapper(textField: MooableTextField) : MooableTextField by textField

fun box(): String {
    val textField = TextFieldWrapper(SimpleTextField())
    textField.setText("hello world!")

    if (!textField.getText().equals("hello world!")) return "FAIL #!1"
    if (textField.moo(1, 2, 3) != 6) return "FAIL #2"

    return "OK"
}
