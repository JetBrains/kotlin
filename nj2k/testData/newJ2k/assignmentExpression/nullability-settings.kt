// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true

internal class Foo {
    fun foo(o: HashSet<*>) {
        val o2: HashSet<*> = o
        var foo: Int = 0
        foo = o2.size
    }
}