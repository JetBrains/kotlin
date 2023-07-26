package foo

import bar.*
/*p:baz(C)*/import baz.C

/*p:foo*/val a = /*p:bar p:baz p:foo*/A()
/*p:foo*/var b: /*p:bar p:baz p:foo*/baz.B = /*p:bar p:baz p:baz(B) p:foo*/baz.B()

/*p:foo*/fun function(p: /*p:bar p:baz p:foo*/B): /*p:bar p:baz p:foo*/B {
    /*p:baz p:foo*/a
    return /*p:bar p:baz p:foo*/B()
}

/*p:foo*/fun /*p:bar p:baz p:foo*/MyClass.extFunc(p: /**p:foo p:bar*//*p:bar p:baz p:foo*/Array</*p:bar p:baz p:foo*/B>, e: /*p:bar p:baz p:foo*/MyEnum, c: /**???*//*p:bar p:baz p:foo*/C): /*p:bar p:baz p:foo*/MyInterface {
    /*p:bar p:baz p:foo p:foo.MyClass*/b
    return /*p:bar p:baz p:foo p:foo.MyClass*/MyClass()
}
