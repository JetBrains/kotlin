// KT-2388
package foo

var done = 0

object foo {
    var result = "FAIL"

    val lambda = {
        result = "foo.lambda OK"
        done = 3
    }

    val extLambda: Int.()->Unit = {
        result = "foo.extLambda OK"
        done = this
    }
}

class Foo {
    var result = "FAIL"

    val lambda = {
        result = "Foo::lambda OK"
        done = -7
    }

    val extLambda: Int.()->Unit = {
        result = "Foo::extLambda OK"
        done = this
    }
}

fun box(): String {
    val a = foo.lambda
    val b = foo.extLambda

    val f = Foo()
    val c = f.lambda
    val d = f.extLambda

    a()
    if (foo.result != "foo.lambda OK") return "foo.result = \"${foo.result}\", but expected \"foo.lambda OK\""
    if (done != 3) return "done = $done, but expected 3"

    23.b()
    if (foo.result != "foo.extLambda OK") return "foo.result = \"${foo.result}\", but expected \"foo.extLambda OK\""
    if (done != 23) return "done = $done, but expected 23"


    c()
    if (f.result != "Foo::lambda OK") return "a.result = \"${f.result}\", but expected \"Foo::lambda OK\""
    if (done != -7) return "done = $done, but expected -7"

    71.d()
    if (f.result != "Foo::extLambda OK") return "a.result = \"${f.result}\", but expected \"Foo::extLambda OK\""
    if (done != 71) return "done = $done, but expected 71"

    return "OK"
}
