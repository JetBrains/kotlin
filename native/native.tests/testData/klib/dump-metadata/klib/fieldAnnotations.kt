package test

annotation class Ann

@field:Ann
var x: Int = 5
@delegate:Ann
var y: Int by ::x

class A {
    @field:Ann
    var x: Int = 5
    @delegate:Ann
    var y: Int by ::x
}
