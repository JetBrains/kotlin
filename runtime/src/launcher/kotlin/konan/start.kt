import konan.internal.ExportForCppRuntime

external fun main(args: Array<String>)

@ExportForCppRuntime
private fun Konan_start(args: Array<String>): Int {
    try {

        // This is kotlin program main entry point
        main(args)

        // Successfully finished:
        return 0

    } catch (e: Throwable) {
        // TODO: may be add some more info.
        print("Uncaught exception from Kotlin's main: ")
        e.printStackTrace()
        return 1
    }
}
