/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
