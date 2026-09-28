// A suspend lambda with a non-tail suspend call: WasmSuspendLambdaMergingLowering replaces its
// coroutine class with a file-local shared class (`SuspendLambda_*`), which is expected to be
// merged at link time with the structurally identical one from b.kt.
fun a(x: Any): suspend () -> Any = { id(x); x }
