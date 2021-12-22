package foo
/*p:bar(C)*/import bar.C
/*p:bar(SAMInterface)*/import bar.SAMInterface

/*p:foo*/fun foo(c: /*p:bar*/C) /*p:bar(SAMInterface)*/{
    /*p:bar(C)*/c./*c:bar.C c:bar.C(getFOO) c:bar.C(getFoo) p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/foo()
    /*p:bar(C)*/c./*c:bar.C c:bar.SAMInterface(<SAM-CONSTRUCTOR>)*/foo /*p:kotlin(Function1) p:kotlin(String)*/{  }

    /*p:bar p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*c:bar.C*/bar()
    /*p:bar p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/C./*c:bar.C c:bar.SAMInterface(<SAM-CONSTRUCTOR>)*/bar /*p:kotlin(Function1) p:kotlin(String)*/{}

    /*c:bar.SAMInterface(<SAM-CONSTRUCTOR>) p:bar p:foo p:java.lang p:kotlin p:kotlin.annotation p:kotlin.collections p:kotlin.comparisons p:kotlin.io p:kotlin.jvm p:kotlin.ranges p:kotlin.sequences p:kotlin.text*/SAMInterface()
    /*c:bar.SAMInterface(<SAM-CONSTRUCTOR>) p:bar*/SAMInterface /*p:kotlin(Function1) p:kotlin(String)*/{}
}
