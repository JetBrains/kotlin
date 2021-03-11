package test

class /*rename*/SameScopeA { val contentA = 1 }
class SameScopeB { val contentB = 2 }

fun useClass() {
    val va = SameScopeA().contentA
    val vb = SameScopeB().contentB
}