/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

enum class Color {
    RED, GREEN, BLUE, CYAN, MAGENTA, YELLOW
}

fun determineColor(code: Int): Color = when (code) {
    0 -> Color.BLUE
    1 -> Color.MAGENTA
    else -> Color.CYAN
}