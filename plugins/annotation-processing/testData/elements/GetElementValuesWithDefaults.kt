annotation class Anno(val name: String = "Mary", val age: Int = 20)

@Anno("Tom", 10)
class A

@Anno(name = "Tom")
class B

@Anno(age = 10)
class C

@Anno
class D