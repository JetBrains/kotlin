
@file:Import("kt86352-import.main.kts")

class MyData(val value: String)

val result = MyData("hello").typeName()

println("result = $result")
