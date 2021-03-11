// "Add 'abstract val hoge: Int' to 'Foo'" "true"
abstract class Foo

class Bar: Foo() {
    override<caret> val hoge = 3
}