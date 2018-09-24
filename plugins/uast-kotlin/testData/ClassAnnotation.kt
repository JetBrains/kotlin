@Test
class A

annotation class MyAnnotation(val text: String)

@MyAnnotation("class")
class B {

    @MyAnnotation("inB class")
    class InB {

    }

    @MyAnnotation("companion")
    companion object {

    }

}

@MyAnnotation("object")
object Obj