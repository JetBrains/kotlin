package foo
/*p:bar(C)*/import bar.C
/*p:bar(SAMInterface)*/import bar.SAMInterface

/*p:foo*/fun foo(c: /*p:bar*/C) /*p:bar(SAMInterface)*/{
    /*p:bar(C)*/c./*c:bar.C c:bar.SAMInterface(<SAM-CONSTRUCTOR>) c:bar.C(getFoo) c:bar.C(getFOO) p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.jvm p:java.lang*/foo()
    /*p:bar(C)*/c./*c:bar.C c:bar.SAMInterface(<SAM-CONSTRUCTOR>)*/foo /*p:kotlin(Function1) p:kotlin(String)*/{  }

    /*p:bar p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.jvm p:java.lang*/C./*c:bar.C c:bar.SAMInterface(<SAM-CONSTRUCTOR>)*/bar()
    /*p:bar p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.jvm p:java.lang*/C./*c:bar.C c:bar.SAMInterface(<SAM-CONSTRUCTOR>)*/bar /*p:kotlin(Function1) p:kotlin(String)*/{}

    /*p:bar c:bar.SAMInterface(<SAM-CONSTRUCTOR>) p:foo p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.ranges p:kotlin.sequences p:kotlin.text p:kotlin.io p:kotlin.comparisons p:kotlin.jvm p:java.lang*/SAMInterface()
    /*p:bar c:bar.SAMInterface(<SAM-CONSTRUCTOR>)*/SAMInterface /*p:kotlin(Function1) p:kotlin(String)*/{}
}
