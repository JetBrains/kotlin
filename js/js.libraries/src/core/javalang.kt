/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package java.lang

@library
public interface Runnable {
    public fun run(): Unit
}

public fun Runnable(action: () -> Unit): Runnable = object : Runnable {
    override fun run() = action()
}

@library
public interface Appendable {
    public fun append(csq: CharSequence?): Appendable
    public fun append(csq: CharSequence?, start: Int, end: Int): Appendable
    public fun append(c: Char): Appendable
}

@library
public class StringBuilder(content: String = "") : Appendable, CharSequence {
    override val length: Int = noImpl
    override fun get(index: Int): Char = noImpl
    override fun subSequence(start: Int, end: Int): CharSequence = noImpl
    override fun append(c: Char): StringBuilder = noImpl
    override fun append(csq: CharSequence?): StringBuilder = noImpl
    override fun append(csq: CharSequence?, start: Int, end: Int): StringBuilder = noImpl
    public fun append(obj: Any?): StringBuilder = noImpl
    public fun reverse(): StringBuilder = noImpl
    override fun toString(): String = noImpl
}

public inline fun StringBuilder(capacity: Int): StringBuilder = StringBuilder()
public inline fun StringBuilder(content: CharSequence): StringBuilder = StringBuilder(content.toString())
