package foo

import bar.*
/*p:baz(C)*/import baz.C

/*p:foo*/val a = /*p:bar p:foo*/A()
/*p:foo*/var b: /*p:bar p:bar.baz(B) p:baz(B) p:foo p:foo.baz(B)*/baz.B = /*p:bar p:baz(B) p:foo*/baz.B()

/*p:foo*/fun function(p: /*p:bar p:foo*/B): /*p:bar p:foo*/B {
    /*p:bar(A) p:foo*/a
    return /*p:bar p:foo*/B()
}

/*p:foo*/fun /*p:bar p:foo*/MyClass.extFunc(p: /**p:foo p:bar*//*p:bar p:bar(B) p:foo p:kotlin*/Array</*p:bar p:foo*/B>, e: /*p:bar p:foo*/MyEnum, c: /**???*//*p:bar p:baz p:foo*/C): /*p:bar p:foo*/MyInterface {
    /*p:bar p:baz(B) p:foo p:foo.MyClass*/b
    return /*p:bar p:foo p:foo.MyClass*/MyClass()
}
