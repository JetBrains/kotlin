package sample

// Arity-1 suspend function. Forces SuspendFunction1 (<: Function2)
// to be loaded and the AddFunctionSupertypeToSuspendFunctionLowering supertypes to be synthesized.
suspend fun target(x: Int): Int = x + 1
