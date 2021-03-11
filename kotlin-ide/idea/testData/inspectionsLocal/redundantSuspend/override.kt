// PROBLEM: none

interface I {
    suspend fun f()
}

class C : I {
    override <caret>suspend fun f() {
    }
}