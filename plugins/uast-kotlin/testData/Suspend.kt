package test.pkg

class Context {
    suspend fun inner(): Int = suspendPrivate()
    private suspend fun suspendPrivate(): Int = inner()
}


suspend fun top(): Int = Context().inner()
