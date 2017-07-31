package foo

/*p:foo*/class C

/*p:foo*/fun lambdaConsumer(fn: (/*p:foo*/A)->/*p:foo p:kotlin*/Unit) {}
/*p:foo*/fun extensionConsumer(fn: /*p:foo*/A.()->/*p:foo p:kotlin*/Unit) {}

/*p:foo*/fun testLambdaParameterType() {
    /*p:foo*/lambdaConsumer /*p:kotlin(Function1) p:foo(A)*/{ /*p:foo(A)*/it }
    /*p:foo*/extensionConsumer /*p:kotlin(Function1) p:foo(A)*/{ /*p:foo(A)*/this }
}