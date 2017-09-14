package foo

import bar.*
/*p:baz(C)*/import baz.C

/*p:foo*/val a = /*p:foo p:bar*/A()
/*p:foo*/var b: /*p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm*/baz./*p:baz*/B = /*p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm p:baz(B)*/baz./*p:baz*/B()

/*p:foo*/fun function(p: /*p:foo p:bar*/B): /*p:foo p:bar*/B /*p:kotlin(Nothing)*/{
    /*p:foo p:bar(A)*/a
    /*p:kotlin(Nothing)*/return /*p:foo p:bar*/B()
}

/*p:foo*/fun /*p:foo*/MyClass.extFunc(p: /**p:foo p:bar*//*p:foo p:kotlin*/Array</*p:foo p:bar*/B>, e: /*p:foo*/MyEnum, c: /**???*//*p:baz*/C): /*p:foo*/MyInterface /*p:kotlin(Nothing)*/{
    /*c:foo.MyClass c:foo.MyClass(getB) p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm p:baz(B)*/b
    /*p:kotlin(Nothing)*/return /*c:foo.MyClass p:foo p:bar p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:java.lang p:kotlin.jvm*/MyClass()
}
