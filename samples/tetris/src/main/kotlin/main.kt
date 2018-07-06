/**
 * Copyright 2010-2018 JetBrains s.r.o.
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
import tetris.*

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