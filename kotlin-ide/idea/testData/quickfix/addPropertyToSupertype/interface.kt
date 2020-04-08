// "Add 'val hoge: Int' to 'Foo'" "true"
interface Foo

class Bar: Foo {
    override<caret> val hoge = 3
}