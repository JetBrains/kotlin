package foo

/*p:foo*/class C

/*p:foo*/fun lambdaConsumer(fn: /*p:foo(A) p:kotlin(Function1)*/(/*p:foo*/A)->/*p:foo*/Unit) {}
/*p:foo*/fun extensionConsumer(fn: /*p:foo p:kotlin(Function1)*/A.()->/*p:foo*/Unit) {}

/*p:foo*/fun testLambdaParameterType() {
    /*p:foo*/lambdaConsumer /*p:foo(A) p:kotlin(Function1)*/{ /*p:foo(A)*/it }
    /*p:foo*/extensionConsumer /*p:foo(A) p:kotlin(Function1)*/{ this }
}
