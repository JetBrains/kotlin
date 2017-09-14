package foo

import bar.*
/*p:baz(C)*/import baz.C

/*p:foo*/val a = /*p:foo p:bar*/A()
/*p:foo*/var b: /*p:foo p:bar*/baz./*p:baz*/B = /*p:foo p:bar p:baz(B)*/baz./*p:baz*/B()

/*p:foo*/fun function(p: /*p:foo p:bar*/B): /*p:foo p:bar*/B /*p:kotlin(Nothing)*/{
    /*p:foo p:bar(A)*/a
    /*p:kotlin(Nothing)*/return /*p:foo p:bar*/B()
}

/*p:foo*/fun /*p:foo*/MyClass.extFunc(p: /**p:foo p:bar*//*p:foo p:bar*/Array</*p:foo p:bar*/B>, e: /*p:foo*/MyEnum, c: /**???*//*p:baz*/C): /*p:foo*/MyInterface /*p:kotlin(Nothing)*/{
    /*c:foo.MyClass p:foo p:bar p:baz(B)*/b
    /*p:kotlin(Nothing)*/return /*c:foo.MyClass p:foo p:bar*/MyClass()
}
