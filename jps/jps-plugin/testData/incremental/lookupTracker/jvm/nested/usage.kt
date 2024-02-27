
/*p:<root>*/fun usageAsCall(): /*p:<root>*/Int {
    val f = /*p:<root> p:MainClass(NestedClass) p:MainClass.NestedClass(A) p:MainClass.NestedClass.A(B) p:MainClass.NestedClass.A.B(F)*/MainClass.NestedClass./*p:MainClass.NestedClass*/A.B./*p:MainClass.NestedClass.A.B*/F
    return /*p:<root> p:MainClass(NestedClass) p:MainClass.NestedClass(A) p:MainClass.NestedClass.A(B) p:MainClass.NestedClass.A.B(F) p:MainClass.NestedClass.A.B.F(x)*/MainClass.NestedClass./*p:MainClass.NestedClass*/A.B./*p:MainClass.NestedClass.A.B*/F.x
}

/*p:<root>*/fun usageAsType(f: /*p:<root> p:MainClass(NestedClass) p:MainClass.NestedClass(A) p:MainClass.NestedClass.A(B) p:MainClass.NestedClass.A.B(F)*/MainClass.NestedClass.A.B.F) = /*p:MainClass.NestedClass.A.B(F)*/f
