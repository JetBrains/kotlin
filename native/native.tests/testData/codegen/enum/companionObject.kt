/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

enum class Game {
    ROCK,
    PAPER,
    SCISSORS;

    companion object {
        fun foo() = ROCK
        val bar = PAPER
        val values2 = values()
        val scissors = valueOf("SCISSORS")
    }
}

fun box(): String {
    if (Game.foo() != Game.ROCK) return "Fail 1"
    if (Game.bar != Game.PAPER) return "Fail 2: ${Game.bar}"
    if (Game.values().size != 3) return "Fail 3"
    if (Game.valueOf("SCISSORS") != Game.SCISSORS) return "Fail 4"
    if (Game.values2.size != 3) return "Fail 5"
    if (Game.scissors != Game.SCISSORS) return "Fail 6"
    return "OK"
}
