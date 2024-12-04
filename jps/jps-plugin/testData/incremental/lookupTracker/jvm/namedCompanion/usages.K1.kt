/*p:<root>*/fun Explicit() /*p:MainClass(Name)*/{
    /*p:<root> p:MainClass(Name)*/MainClass./*p:MainClass p:MainClass.Name*/Name
}

/*p:<root>*/fun ExplicitMethod() {
    /*p:<root>*/MainClass./*p:MainClass*/Name./*p:MainClass.Name*/f()
}

/*p:<root>*/fun Implicit() /*p:MainClass(Name)*/{
    /*p:<root> p:MainClass(Name)*/MainClass
}

/*p:<root>*/fun ImplicitMethod() {
    /*p:<root>*/MainClass./*p:MainClass p:MainClass.Name*/f()
}

/*p:<root>*/fun InstanceExplicit() {
    val t = /*p:<root> p:MainClass(Name)*/MainClass./*p:MainClass p:MainClass.Name*/Name
}

/*p:<root>*/fun Type(t: /*p:<root>*/MainClass./*p:MainClass*/Name) {

}
