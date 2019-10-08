import Foo.SomeClass
import java.util.ArrayList

internal interface FooInterface {
    fun foo(): ArrayList<out SomeClass?>?
}

class Foo : FooInterface {
    override fun foo(): ArrayList<SomeClass?>? {
        return null
    }

    class SomeClass
}