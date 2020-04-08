annotation class SuperAnnotation

class TestClass(text: String) {
    @SuperAnnotation val <caret>text = text
}