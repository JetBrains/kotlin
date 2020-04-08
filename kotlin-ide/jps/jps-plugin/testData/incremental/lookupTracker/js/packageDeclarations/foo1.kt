package foo

import bar.*
/*p:baz(C)*/import baz.C

/*p:foo*/val a = /*p:bar p:foo*/A()
/*p:foo*/var b: /*p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/baz./*p:baz*/B = /*p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js p:baz(B)*/baz./*p:baz*/B()

/*p:foo*/fun function(p: /*p:foo p:bar*/B): /*p:foo p:bar*/B /*p:kotlin(Nothing)*/{
    /*p:foo p:bar(A)*/a
    /*p:kotlin(Nothing)*/return /*p:bar p:foo*/B()
}

/*p:foo*/fun /*p:foo*/MyClass.extFunc(p: /**p:foo p:bar*//*p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/Array</*p:foo p:bar*/B>, e: /*p:foo*/MyEnum, c: /**???*//*p:baz*/C): /*p:foo*/MyInterface /*p:kotlin(Nothing)*/{
    /*c:foo.MyClass p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js p:baz(B)*/b
    /*p:kotlin(Nothing)*/return /*c:foo.MyClass p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.js*/MyClass()
}
