package foo
/*p:bar(C)*/import bar.C
/*p:bar(SAMInterface)*/import bar.SAMInterface

/*p:foo*/fun foo(c: /*p:bar p:foo*/C) {
    /*p:bar(C) p:bar.C(foo) p:foo(foo)*/c.foo()
    /*p:bar(C) p:bar(SAMInterface) p:bar.C(foo) p:bar.SAMInterface(<SAM-CONSTRUCTOR>)*/c.foo /*p:kotlin(Function1)*/{  }

    /*p:bar.C(bar) p:foo p:foo(bar)*/C.bar()
    /*p:bar(SAMInterface) p:bar.C(bar) p:bar.SAMInterface(<SAM-CONSTRUCTOR>) p:foo*/C.bar /*p:kotlin(Function1)*/{}

    /*p:bar p:foo*/SAMInterface()
    /*p:bar*/SAMInterface /*p:kotlin(Function1)*/{}
}
