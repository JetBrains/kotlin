/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package demo

class Session(val name: String, val number: Int)

class Server(val prefix: String) {
    fun greet(session: Session) = "$prefix: Hello from Kotlin/Native in ${session}"
    fun concat(session: Session, a: String, b: String) = "$prefix: $a $b in ${session}"
    fun add(session: Session, a: Int, b: Int) = a + b + session.number
}
