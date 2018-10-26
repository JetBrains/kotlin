/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.tetris

fun main(args: Array<String>) {
    var startLevel = Config.startLevel
    var width = Config.width
    var height = Config.height
    when (args.size) {
        1 -> startLevel = args[0].toInt()
        2 -> {
            width = args[0].toInt()
            height = args[1].toInt()
        }
        3 -> {
            width = args[0].toInt()
            height = args[1].toInt()
            startLevel = args[2].toInt()
        }
    }
    val visualizer = SDL_Visualizer(width, height)
    val game = Game(width, height, visualizer, visualizer)
    game.startNewGame(startLevel)

    return
}