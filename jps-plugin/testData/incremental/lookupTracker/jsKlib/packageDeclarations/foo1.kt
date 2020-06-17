package foo

import bar.*
/*p:baz(C)*/import baz.C

/*p:foo*/val a = /*p:bar p:foo*/A()
/*p:foo*/var b: /*p:bar p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/baz./*p:baz*/B = /*p:bar p:baz(B) p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/baz./*p:baz*/B()

/*p:foo*/fun function(p: /*p:bar p:foo*/B): /*p:bar p:foo*/B /*p:kotlin(Nothing)*/{
    /*p:bar(A) p:foo*/a
    /*p:kotlin(Nothing)*/return /*p:bar p:foo*/B()
}

/*p:foo*/fun /*p:foo*/MyClass.extFunc(p: /**p:foo p:bar*//*p:bar p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/Array</*p:bar p:foo*/B>, e: /*p:foo*/MyEnum, c: /**???*//*p:baz*/C): /*p:foo*/MyInterface /*p:kotlin(Nothing)*/{
    /*c:foo.MyClass p:bar p:baz(B) p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/b
    /*p:kotlin(Nothing)*/return /*c:foo.MyClass p:bar p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.js p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/MyClass()
}
