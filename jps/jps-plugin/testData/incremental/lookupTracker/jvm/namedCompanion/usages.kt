/*p:<root>*/fun Explicit() {
    /*p:<root> p:MainClass(Name)*/MainClass./*p:MainClass*/Name
}

/*p:<root>*/fun ExplicitMethod() {
    /*p:<root> p:<root>(f) p:MainClass(Name) p:MainClass.Name(f)*/MainClass./*p:MainClass*/Name.f()
}

/*p:<root>*/fun Implicit() {
    /*p:<root> p:MainClass(Name)*/MainClass
}

/*p:<root>*/fun ImplicitMethod() {
    /*p:<root> p:<root>(f) p:MainClass(Name) p:MainClass(f) p:MainClass.Name(f)*/MainClass.f()
}

/*p:<root>*/fun InstanceExplicit() {
    val t = /*p:<root> p:MainClass(Name)*/MainClass./*p:MainClass*/Name
}

/*p:<root>*/fun Type(t: /*p:<root> p:MainClass(Name)*/MainClass.Name) {

}
