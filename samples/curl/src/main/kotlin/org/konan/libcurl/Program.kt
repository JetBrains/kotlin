package org.konan.libcurl

fun main(args: Array<String>) {
    if (args.size == 0)
        return help()

    val curl = CUrl(args[0])
    curl.header += {
        println("[H] $it")
    }

    curl.body += {
        println("[B] $it")
    }

    curl.fetch()
    curl.close()


}

fun help() {
    println("ERROR: missing URL command line argument")
}

