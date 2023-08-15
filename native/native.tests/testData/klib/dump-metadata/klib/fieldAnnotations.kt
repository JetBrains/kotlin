// KT-57135 K2 dosen't take into account `field` target on annotation for property
// MUTED_WHEN: K2
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
