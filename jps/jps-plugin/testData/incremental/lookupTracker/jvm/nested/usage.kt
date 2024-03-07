
package one

/*p:one*/fun usageAsCall(): /*p:one*/Int {
    val f = /*p:one p:one.MainClass(NestedClass) p:one.MainClass.NestedClass(A) p:one.MainClass.NestedClass.A(B) p:one.MainClass.NestedClass.A.B(F)*/MainClass.NestedClass./*p:one.MainClass.NestedClass*/A.B./*p:one.MainClass.NestedClass.A.B*/F
    return /*p:one p:one.MainClass(NestedClass) p:one.MainClass.NestedClass(A) p:one.MainClass.NestedClass.A(B) p:one.MainClass.NestedClass.A.B(F) p:one.MainClass.NestedClass.A.B.F(x)*/MainClass.NestedClass./*p:one.MainClass.NestedClass*/A.B./*p:one.MainClass.NestedClass.A.B*/F.x
}

/*p:one*/fun usageAsType(f: /*p:one p:one.MainClass(NestedClass) p:one.MainClass.NestedClass(A) p:one.MainClass.NestedClass.A(B) p:one.MainClass.NestedClass.A.B(F)*/MainClass.NestedClass.A.B.F) = /*p:one.MainClass.NestedClass.A.B(F)*/f
