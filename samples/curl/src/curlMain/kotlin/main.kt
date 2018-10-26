/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.curl

import sample.libcurl.*

fun main(args: Array<String>) {
    if (args.isEmpty())
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

