package foo
/*p:bar(C)*/import bar.C
/*p:bar(SAMInterface)*/import bar.SAMInterface

/*p:foo*/fun foo(c: /*p:bar p:foo*/C) {
    /*p:C(foo) p:bar(foo) p:bar.C(foo) p:foo(foo) p:kotlin(Unit)*/c.foo()
    /*p:C(foo) p:bar(SAMInterface) p:bar.C(foo) p:bar/SAMInterface(<SAM-CONSTRUCTOR>) p:kotlin(Unit)*/c.foo /*p:kotlin(Function1) p:kotlin(Unit)*/{  /*p:kotlin(Unit)*/}

    /*p:bar p:bar(bar) p:foo p:foo(bar) p:kotlin(Unit)*/C.bar()
    /*p:bar p:bar(SAMInterface) p:bar/SAMInterface(<SAM-CONSTRUCTOR>) p:foo p:kotlin(Unit)*/C.bar /*p:kotlin(Function1) p:kotlin(Unit)*/{/*p:kotlin(Unit)*/}

    /*p:bar p:foo*/SAMInterface()
    /*p:bar*/SAMInterface /*p:kotlin(Function1) p:kotlin(Unit)*/{/*p:kotlin(Unit)*/}
}
