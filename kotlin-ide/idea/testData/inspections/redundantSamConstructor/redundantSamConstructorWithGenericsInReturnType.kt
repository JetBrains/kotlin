package redundantSamConstructor

import a.*

fun testGenericsReturnType() {
    val runnable = JFunction0 { "" }
    val klass = GenericClass2()

    GenericClass2.staticFun1(JFunction0 { "" })
    GenericClass2.staticFun1(runnable)
    GenericClass2.staticFun2(JFunction0 { "" }, JFunction0 { "" })
    GenericClass2.staticFun2(runnable, JFunction0 { "" })
    GenericClass2.staticFun2({ -> "" }, { -> "" })

    GenericClass2.staticFunWithOtherParam(1, JFunction0 { "" })
    GenericClass2.staticFunWithOtherParam(1, runnable)

    klass.memberFun1(JFunction0 { "" })
    klass.memberFun1(runnable)
    klass.memberFun2(JFunction0 { "" }, JFunction0 { "" })
    klass.memberFun2(runnable, JFunction0 { "" })
    klass.memberFun2({ -> "" }, { -> "" })

    klass.memberFunWithOtherParam(1, JFunction0 { "" })
    klass.memberFunWithOtherParam(1, runnable)
}