package foo

import bar.*
import baz./*p:baz*/C

/*p:foo*/val a = /*p:foo p:bar*/A()
/*p:foo*/var b: /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/baz./*p:baz*/B = /*p:foo p:bar p:java.lang p:kotlin p:kotlin.annotation p:kotlin.jvm p:kotlin.io*/baz./*p:baz*/B()

/*p:foo*/fun function(p: /*p:foo p:bar*/B): /*p:foo p:bar*/B {
    /*p:foo*/a
    return /*p:foo p:bar*/B()
}

/*p:foo*/fun /*p:foo*/MyClass.extFunc(p: /**p:foo p:bar*//*p:foo*/Array</*p:foo p:bar*/B>, e: /*p:foo*/MyEnum, c: /**???*/C): /*p:foo*/MyInterface {
    /*c:foo.MyClass p:foo*/b
    return /*c:foo.MyClass p:foo*/MyClass()
}
