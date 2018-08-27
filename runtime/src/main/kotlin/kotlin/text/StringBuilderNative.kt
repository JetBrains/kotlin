/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

fun StringBuilder.appendln(it: String) = append(it).appendln()
fun StringBuilder.appendln(it: Boolean) = append(it).appendln()
fun StringBuilder.appendln(it: Byte) = append(it).appendln()
fun StringBuilder.appendln(it: Short) = append(it).appendln()
fun StringBuilder.appendln(it: Int) = append(it).appendln()
fun StringBuilder.appendln(it: Long) = append(it).appendln()
fun StringBuilder.appendln(it: Float) = append(it).appendln()
fun StringBuilder.appendln(it: Double) = append(it).appendln()
fun StringBuilder.appendln(it: Any?) = append(it).appendln()

fun StringBuilder.appendln() = append('\n')
