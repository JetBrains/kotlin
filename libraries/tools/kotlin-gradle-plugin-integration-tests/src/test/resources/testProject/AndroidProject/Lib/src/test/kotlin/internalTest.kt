import lib.internalLibFun

fun main(args: Array<String>) {
    // check that unit tests can access internals of the main variant:
    internalLibFun()
}