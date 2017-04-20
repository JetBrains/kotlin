package org.konan.libcurl

fun main(args: Array<String>) {
    if (args.size == 0)
        return help()

    val curl = CUrl(args[0])
    curl.header += {
        println("[H] $it")
    }
    curl.fetch()
    curl.close()

/*
    val write_data = staticCFunction(::write_data)
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);
*/
}

fun help() {
    println("ERROR: missing URL command line argument")
}

