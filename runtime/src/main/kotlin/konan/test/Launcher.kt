package konan.test

import kotlin.system.exitProcess

fun main(args:Array<String>) {
    val exitCode = TestRunner.run(args)
    exitProcess(exitCode)
}
