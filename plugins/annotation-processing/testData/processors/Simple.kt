annotation class Anno(val name: String)

@Anno("MyClassAnno")
class MyClass {
    @Anno("myFuncAnno")
    fun myFunc(): String = "Mary"
    
    @field:Anno("myFieldAnno")
    val myField: String = "Tom"
}