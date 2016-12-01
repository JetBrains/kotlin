
external fun main(args: Array<String>)

fun start(args: Array<String>) {
    try {

        // This is kotlin program main entry point
        main(args)

    } catch (e: Throwable) {
        // TODO: Remove .toString() when println is more capabe, 
        // and may be add some more info.
        print("Uncaught exception from kotlin main: ")
        println(e.toString())
    }
}
