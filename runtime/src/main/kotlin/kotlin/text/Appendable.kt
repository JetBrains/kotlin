/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.text

public actual interface Appendable {
    actual fun append(c: Char): Appendable
    actual fun append(csq: CharSequence?): Appendable
    actual fun append(csq: CharSequence?, start: Int, end: Int): Appendable
}
