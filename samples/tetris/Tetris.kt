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

import kotlinx.cinterop.*
import sdl.*

typealias Field = Array<ByteArray>

enum class Move {
    LEFT,
    RIGHT,
    DOWN,
    ROTATE
}

enum class PlacementResult(val linesCleared: Int = 0, val bonus: Int = 0) {
    NOTHING,
    GAMEOVER,
    // For values of bonuses see https://tetris.wiki/Scoring
    SINGLE(1, 40),
    DOUBLE(2, 100),
    TRIPLE(3, 300),
    TETRIS(4, 1200)
}

const val EMPTY: Byte = 0
const val CELL1: Byte = 1
const val CELL2: Byte = 2
const val CELL3: Byte = 3
const val BRICK: Byte = -1

class Point(var x: Int, var y: Int)

operator fun Point.plus(other: Point): Point {
    return Point(x + other.x, y + other.y)
}

class PiecePosition(piece: Piece, private val origin: Point) {
    private var p = piece.origin
    val x get() = p.x + origin.x
    val y get() = p.y + origin.y

    var state: Int get private set
    val numberOfStates = piece.numberOfStates

    init {
        state = 0
    }

    fun makeMove(move: Move) {
        when (move) {
            Move.LEFT -> --p.y
            Move.RIGHT -> ++p.y
            Move.DOWN -> ++p.x
            Move.ROTATE -> state = (state + 1) % numberOfStates
        }
    }

    fun unMakeMove(move: Move) {
        when (move) {
            Move.LEFT -> ++p.y
            Move.RIGHT -> --p.y
            Move.DOWN -> --p.x
            Move.ROTATE -> state = (state + numberOfStates - 1) % numberOfStates
        }
    }
}

/*
 * We use Nintendo Rotation System, right-handed version.
 * See https://tetris.wiki/Nintendo_Rotation_System
 */
enum class Piece(private val origin_: Point, private vararg val states: Field) {
    T(Point(-1, -2),
            arrayOf(
                    byteArrayOf(EMPTY, EMPTY, EMPTY),
                    byteArrayOf(CELL1, CELL1, CELL1),
                    byteArrayOf(EMPTY, CELL1, EMPTY)),
            arrayOf(
                    byteArrayOf(EMPTY, CELL1, EMPTY),
                    byteArrayOf(CELL1, CELL1, EMPTY),
                    byteArrayOf(EMPTY, CELL1, EMPTY)),
            arrayOf(
                    byteArrayOf(EMPTY, CELL1, EMPTY),
                    byteArrayOf(CELL1, CELL1, CELL1),
                    byteArrayOf(EMPTY, EMPTY, EMPTY)),
            arrayOf(
                    byteArrayOf(EMPTY, CELL1, EMPTY),
                    byteArrayOf(EMPTY, CELL1, CELL1),
                    byteArrayOf(EMPTY, CELL1, EMPTY))
    ),
    J(Point(-1, -2),
            arrayOf(
                    byteArrayOf(EMPTY, EMPTY, EMPTY),
                    byteArrayOf(CELL2, CELL2, CELL2),
                    byteArrayOf(EMPTY, EMPTY, CELL2)),
            arrayOf(
                    byteArrayOf(EMPTY, CELL2, EMPTY),
                    byteArrayOf(EMPTY, CELL2, EMPTY),
                    byteArrayOf(CELL2, CELL2, EMPTY)),
            arrayOf(
                    byteArrayOf(CELL2, EMPTY, EMPTY),
                    byteArrayOf(CELL2, CELL2, CELL2),
                    byteArrayOf(EMPTY, EMPTY, EMPTY)),
            arrayOf(
                    byteArrayOf(EMPTY, CELL2, CELL2),
                    byteArrayOf(EMPTY, CELL2, EMPTY),
                    byteArrayOf(EMPTY, CELL2, EMPTY))
    ),
    Z(Point(-1, -2),
            arrayOf(
                    byteArrayOf(EMPTY, EMPTY, EMPTY),
                    byteArrayOf(CELL3, CELL3, EMPTY),
                    byteArrayOf(EMPTY, CELL3, CELL3)),
            arrayOf(
                    byteArrayOf(EMPTY, EMPTY, CELL3),
                    byteArrayOf(EMPTY, CELL3, CELL3),
                    byteArrayOf(EMPTY, CELL3, EMPTY))
    ),
    O(Point(0, -1),
            arrayOf(
                    byteArrayOf(CELL1, CELL1),
                    byteArrayOf(CELL1, CELL1))
    ),
    S(Point(-1, -2),
            arrayOf(
                    byteArrayOf(EMPTY, EMPTY, EMPTY),
                    byteArrayOf(EMPTY, CELL2, CELL2),
                    byteArrayOf(CELL2, CELL2, EMPTY)),
            arrayOf(
                    byteArrayOf(EMPTY, CELL2, EMPTY),
                    byteArrayOf(EMPTY, CELL2, CELL2),
                    byteArrayOf(EMPTY, EMPTY, CELL2))
    ),
    L(Point(-1, -2),
            arrayOf(
                    byteArrayOf(EMPTY, EMPTY, EMPTY),
                    byteArrayOf(CELL3, CELL3, CELL3),
                    byteArrayOf(CELL3, EMPTY, EMPTY)),
            arrayOf(
                    byteArrayOf(CELL3, CELL3, EMPTY),
                    byteArrayOf(EMPTY, CELL3, EMPTY),
                    byteArrayOf(EMPTY, CELL3, EMPTY)),
            arrayOf(
                    byteArrayOf(EMPTY, EMPTY, CELL3),
                    byteArrayOf(CELL3, CELL3, CELL3),
                    byteArrayOf(EMPTY, EMPTY, EMPTY)),
            arrayOf(
                    byteArrayOf(EMPTY, CELL3, EMPTY),
                    byteArrayOf(EMPTY, CELL3, EMPTY),
                    byteArrayOf(EMPTY, CELL3, CELL3))
    ),
    I(Point(-2, -2),
            arrayOf(
                    byteArrayOf(EMPTY, EMPTY, EMPTY, EMPTY),
                    byteArrayOf(EMPTY, EMPTY, EMPTY, EMPTY),
                    byteArrayOf(CELL1, CELL1, CELL1, CELL1),
                    byteArrayOf(EMPTY, EMPTY, EMPTY, EMPTY)),
            arrayOf(
                    byteArrayOf(EMPTY, EMPTY, CELL1, EMPTY),
                    byteArrayOf(EMPTY, EMPTY, CELL1, EMPTY),
                    byteArrayOf(EMPTY, EMPTY, CELL1, EMPTY),
                    byteArrayOf(EMPTY, EMPTY, CELL1, EMPTY))
    );

    val origin get() = Point(origin_.x, origin_.y)
    val numberOfStates: Int = states.size

    fun canBePlaced(field: Field, position: PiecePosition): Boolean {
        val piece = states[position.state]
        val x = position.x
        val y = position.y
        for (i in piece.indices) {
            val pieceRow = piece[i]
            val boardRow = field[x + i]
            for (j in pieceRow.indices) {
                if (pieceRow[j] != EMPTY && boardRow[y + j] != EMPTY)
                    return false
            }
        }
        return true
    }

    fun place(field: Field, position: PiecePosition) {
        val piece = states[position.state]
        val x = position.x
        val y = position.y
        for (i in piece.indices) {
            val pieceRow = piece[i]
            for (j in pieceRow.indices) {
                if (pieceRow[j] != EMPTY) field[x + i][y + j] = pieceRow[j]
            }
        }
    }

    fun unPlace(field: Field, position: PiecePosition) {
        val piece = states[position.state]
        val x = position.x
        val y = position.y
        for (i in piece.indices) {
            val pieceRow = piece[i]
            for (j in pieceRow.indices) {
                if (pieceRow[j] != EMPTY) field[x + i][y + j] = EMPTY
            }
        }
    }
}

interface GameFieldVisualizer {
    fun drawCell(x: Int, y: Int, cell: Byte)
    fun drawNextPieceCell(x: Int, y: Int, cell: Byte)
    fun setInfo(linesCleared: Int, level: Int, score: Int, tetrises: Int)
    fun refresh()
}

enum class UserCommand {
    LEFT,
    RIGHT,
    DOWN,
    DROP,
    ROTATE,
    EXIT
}

interface UserInput {
    fun readCommands(): List<UserCommand>
}

fun sleep(millis: Int) {
    SDL_Delay(millis)
}

class GameField(val width: Int, val height: Int, val visualizer: GameFieldVisualizer) {
    private val MARGIN = 4

    private val field: Field
    private val origin: Point
    private val nextPieceField: Field

    init {
        field = Array<ByteArray>(height + MARGIN * 2) { ByteArray(width + MARGIN * 2) }
        for (i in field.indices) {
            val row = field[i]
            for (j in row.indices) {
                if (i >= (MARGIN + height) // Bottom (field is flipped over).
                        || (j < MARGIN) // Left
                        || (j >= MARGIN + width)) // Right
                    row[j] = BRICK
            }
        }
        // Coordinates are relative to the central axis and top of the field.
        origin = Point(MARGIN, MARGIN + (width + 1) / 2)
        nextPieceField = Array<ByteArray>(4) { ByteArray(4) }
    }

    lateinit var currentPiece: Piece
    lateinit var nextPiece: Piece
    lateinit var currentPosition: PiecePosition

    fun reset() {
        for (i in 0..height - 1)
            for (j in 0..width - 1)
                field[i + MARGIN][j + MARGIN] = 0
        srand(time(null).toInt())
        nextPiece = getNextPiece(false)
        switchCurrentPiece()
    }

    private fun randInt() = (rand() and 32767) or ((rand() and 32767) shl 15)

    private fun getNextPiece(denyPrevious: Boolean): Piece {
        val pieces = Piece.values()
        if (!denyPrevious)
            return pieces[randInt() % pieces.size]
        while (true) {
            val nextPiece = pieces[randInt() % pieces.size]
            if (nextPiece != currentPiece) return nextPiece
        }
    }

    private fun switchCurrentPiece() {
        currentPiece = nextPiece
        nextPiece = getNextPiece(denyPrevious = true) // Forbid repeating the same piece for better distribution.
        currentPosition = PiecePosition(currentPiece, origin)
    }

    fun makeMove(move: Move): Boolean {
        currentPosition.makeMove(move)
        if (currentPiece.canBePlaced(field, currentPosition))
            return true
        currentPosition.unMakeMove(move)
        return false
    }

    /**
     * Places current piece at its current location.
     */
    fun place(): PlacementResult {
        currentPiece.place(field, currentPosition)
        val linesCleared = clearLines()
        if (isOutOfBorders()) return PlacementResult.GAMEOVER
        switchCurrentPiece()
        if (!currentPiece.canBePlaced(field, currentPosition))
            return PlacementResult.GAMEOVER
        when (linesCleared) {
            1 -> return PlacementResult.SINGLE
            2 -> return PlacementResult.DOUBLE
            3 -> return PlacementResult.TRIPLE
            4 -> return PlacementResult.TETRIS
            else -> return PlacementResult.NOTHING
        }
    }

    private fun clearLines(): Int {
        val clearedLines = mutableListOf<Int>()
        for (i in 0..height - 1) {
            val row = field[i + MARGIN]
            if ((0..width - 1).all { j -> row[j + MARGIN] != EMPTY }) {
                clearedLines.add(i + MARGIN)
                (0..width - 1).forEach { j -> row[j + MARGIN] = EMPTY }
            }
        }
        if (clearedLines.size == 0) return 0
        draw(false)
        visualizer.refresh()
        sleep(500)
        for (i in clearedLines) {
            for (k in i - 1 downTo 1)
                for (j in 0..width - 1)
                    field[k + 1][j + MARGIN] = field[k][j + MARGIN]
        }
        draw(false)
        visualizer.refresh()
        return clearedLines.size
    }

    private fun isOutOfBorders(): Boolean {
        for (i in 0..MARGIN - 1)
            for (j in 0..width - 1)
                if (field[i][j + MARGIN] != EMPTY)
                    return true
        return false
    }

    fun draw() {
        draw(true)
        drawNextPiece()
    }

    private fun drawNextPiece() {
        for (i in 0..3)
            for (j in 0..3)
                nextPieceField[i][j] = 0
        nextPiece.place(nextPieceField, PiecePosition(nextPiece, Point(1, 2)))
        for (i in 0..3)
            for (j in 0..3)
                visualizer.drawNextPieceCell(i, j, nextPieceField[i][j])
    }

    private fun draw(drawCurrentPiece: Boolean) {
        if (drawCurrentPiece)
            currentPiece.place(field, currentPosition)
        for (i in 0..height - 1)
            for (j in 0..width - 1)
                visualizer.drawCell(i, j, field[i + MARGIN][j + MARGIN])
        if (drawCurrentPiece)
            currentPiece.unPlace(field, currentPosition)
    }
}

class Game(width: Int, height: Int, val visualizer: GameFieldVisualizer, val userInput: UserInput) {
    private val field = GameField(width, height, visualizer)

    private var gameOver = true
    private var startLevel = 0
    private var leveledUp = false
    private var level = 0
    private var linesClearedAtCurrentLevel = 0
    private var linesCleared = 0
    private var tetrises = 0
    private var score = 0

    /*
     * For speed constants and level up thresholds see https://tetris.wiki/Tetris_(NES,_Nintendo)
     */
    private val speeds = intArrayOf(48, 43, 38, 33, 28, 23, 18, 13, 8, 6, 5, 5, 5, 4, 4, 4, 3, 3, 3,
            2, 2, 2, 2, 2, 2, 2, 2, 2, 2)
    private val levelUpThreshold
        get() =
        if (leveledUp) 10
        else minOf(startLevel * 10 + 10, maxOf(100, startLevel * 10 - 50))
    private val speed get() = if (level < 29) speeds[level] else 1

    private var ticks = 0

    fun startNewGame(level: Int) {
        gameOver = false
        startLevel = level
        leveledUp = false
        this.level = level
        linesClearedAtCurrentLevel = 0
        linesCleared = 0
        tetrises = 0
        score = 0
        ticks = 0
        field.reset()

        visualizer.setInfo(linesCleared, level, score, tetrises)
        field.draw()
        visualizer.refresh()

        mainLoop()
    }

    private fun placePiece() {
        val placementResult = field.place()
        ticks = 0
        when (placementResult) {
            PlacementResult.NOTHING -> return
            PlacementResult.GAMEOVER -> {
                gameOver = true
                return
            }
            else -> {
                linesCleared += placementResult.linesCleared
                linesClearedAtCurrentLevel += placementResult.linesCleared
                score += placementResult.bonus * (level + 1)
                if (placementResult == PlacementResult.TETRIS)
                    ++tetrises
                val levelUpThreshold = levelUpThreshold
                if (linesClearedAtCurrentLevel >= levelUpThreshold) {
                    ++level
                    linesClearedAtCurrentLevel -= levelUpThreshold
                    leveledUp = true
                }

                visualizer.setInfo(linesCleared, level, score, tetrises)
            }
        }
    }

    /*
     * Number of additional gravity shifts before locking a piece landed on the ground.
     * This is needed in order to let user to move a piece to the left/right before locking.
     */
    private val LOCK_DELAY = 1

    private fun mainLoop() {
        var attemptsToLock = 0
        while (!gameOver) {
            sleep(1000 / 60) // Refresh rate - 60 frames per second.
            val commands = userInput.readCommands()
            for (cmd in commands) {
                val success: Boolean
                when (cmd) {
                    UserCommand.EXIT -> return
                    UserCommand.LEFT -> success = field.makeMove(Move.LEFT)
                    UserCommand.RIGHT -> success = field.makeMove(Move.RIGHT)
                    UserCommand.ROTATE -> success = field.makeMove(Move.ROTATE)
                    UserCommand.DOWN -> {
                        success = field.makeMove(Move.DOWN)
                        if (!success) placePiece()
                    }
                    UserCommand.DROP -> {
                        while (field.makeMove(Move.DOWN)) {
                        }
                        success = true
                        placePiece()
                    }
                }
                if (success) {
                    field.draw()
                    visualizer.refresh()
                }
            }
            ++ticks
            if (ticks < speed) continue
            if (!field.makeMove(Move.DOWN)) {
                if (++attemptsToLock >= LOCK_DELAY) {
                    placePiece()
                    attemptsToLock = 0
                }
            }
            field.draw()
            visualizer.refresh()
            ticks -= speed
        }
    }

}

fun get_SDL_Error() = SDL_GetError()!!.toKString()

class SDL_Visualizer(val width: Int, val height: Int): GameFieldVisualizer, UserInput {
    private val CELL_SIZE = 20
    private val COLORS = 10
    private val CELLS_WIDTH = COLORS * CELL_SIZE
    private val CELLS_HEIGHT = 3 * CELL_SIZE
    private val SYMBOL_SIZE = 21
    private val INFO_MARGIN = 10
    private val MARGIN = 2
    private val BORDER_WIDTH = 18
    private val INFO_SPACE_WIDTH = SYMBOL_SIZE * (2 + 8)
    private val LINES_LABEL_WIDTH = 104
    private val SCORE_LABEL_WIDTH = 107
    private val LEVEL_LABEL_WIDTH = 103
    private val NEXT_LABEL_WIDTH = 85
    private val TETRISES_LABEL_WIDTH = 162

    private val ratio: Float

    private fun stretch(value: Int) = (value.toFloat() * ratio + 0.5).toInt()

    inner class GamePadButtons(width: Int, height: Int, gamePadHeight: Int) {
        val MOVE_BUTTON_SIZE = 50
        val ROTATE_BUTTON_SIZE = 80
        val BUTTONS_MARGIN = 25

        val arena = Arena()
        val leftRect: SDL_Rect
        val rightRect: SDL_Rect
        val downRect: SDL_Rect
        val dropRect: SDL_Rect
        val rotateRect: SDL_Rect

        init {
            val moveButtonsWidth = 3 * MOVE_BUTTON_SIZE + 2 * BUTTONS_MARGIN + BUTTONS_MARGIN
            val x = (width - moveButtonsWidth - ROTATE_BUTTON_SIZE) / 2 - MOVE_BUTTON_SIZE
            val y2 = (gamePadHeight - 2 * MOVE_BUTTON_SIZE - BUTTONS_MARGIN) / 2
            leftRect = arena.alloc<SDL_Rect>()
            leftRect.w = MOVE_BUTTON_SIZE
            leftRect.h = MOVE_BUTTON_SIZE
            leftRect.x = x
            leftRect.y = height - gamePadHeight + y2 + MOVE_BUTTON_SIZE + BUTTONS_MARGIN

            downRect = arena.alloc<SDL_Rect>()
            downRect.w = MOVE_BUTTON_SIZE
            downRect.h = MOVE_BUTTON_SIZE
            downRect.x = x + MOVE_BUTTON_SIZE + BUTTONS_MARGIN
            downRect.y = leftRect.y

            dropRect = arena.alloc<SDL_Rect>()
            dropRect.w = MOVE_BUTTON_SIZE
            dropRect.h = MOVE_BUTTON_SIZE
            dropRect.x = downRect.x
            dropRect.y = height - gamePadHeight + y2

            rightRect = arena.alloc<SDL_Rect>()
            rightRect.w = MOVE_BUTTON_SIZE
            rightRect.h = MOVE_BUTTON_SIZE
            rightRect.x = x + 2 * MOVE_BUTTON_SIZE + 2 * BUTTONS_MARGIN
            rightRect.y = height - gamePadHeight + y2 + MOVE_BUTTON_SIZE + BUTTONS_MARGIN

            rotateRect = arena.alloc<SDL_Rect>()
            rotateRect.w = ROTATE_BUTTON_SIZE
            rotateRect.h = ROTATE_BUTTON_SIZE
            rotateRect.x = x + moveButtonsWidth
            rotateRect.y = height - gamePadHeight + y2 - BUTTONS_MARGIN
        }

        fun getCommandAt(x: Int, y: Int): UserCommand? {
            return when {
                inside(leftRect, x, y) -> UserCommand.LEFT
                inside(rightRect, x, y) -> UserCommand.RIGHT
                inside(downRect, x, y) -> UserCommand.DOWN
                inside(dropRect, x, y) -> UserCommand.DROP
                inside(rotateRect, x, y) -> UserCommand.ROTATE
                else -> null
            }
        }

        private fun inside(rect: SDL_Rect, x: Int, y: Int): Boolean {
            return x >= stretch(rect.x) && x <= stretch(rect.x + rect.w)
                   && y >= stretch(rect.y) && y <= stretch(rect.y + rect.h)
        }

        fun destroy() {
            arena.clear()
        }
    }

    private val field: Field = Array<ByteArray>(height) { ByteArray(width) }
    private val nextPieceField: Field = Array<ByteArray>(4) { ByteArray(4) }
    private var linesCleared: Int = 0
    private var level: Int = 0
    private var score: Int = 0
    private var tetrises: Int = 0

    private var displayWidth: Int = 0
    private var displayHeight: Int = 0
    private val fieldWidth: Int
    private val fieldHeight: Int
    private val windowX: Int
    private val windowY: Int
    private val window: CPointer<SDL_Window>
    private val renderer: CPointer<SDL_Renderer>
    private val texture: CPointer<SDL_Texture>
    private val gamePadButtons: GamePadButtons?
    private val platform: String

    init {
        if (SDL_Init(SDL_INIT_EVERYTHING) != 0) {
            println("SDL_Init Error: ${get_SDL_Error()}")
            throw Error()
        }

        platform = SDL_GetPlatform()!!.toKString()

        memScoped {
            val displayMode = alloc<SDL_DisplayMode>()
            if (SDL_GetCurrentDisplayMode(0, displayMode.ptr.reinterpret()) != 0) {
                println("SDL_GetCurrentDisplayMode Error: ${get_SDL_Error()}")
                SDL_Quit()
                throw Error()
            }
            displayWidth = displayMode.w
            displayHeight = displayMode.h
        }
        fieldWidth = width * (CELL_SIZE + MARGIN) + MARGIN + BORDER_WIDTH * 2
        fieldHeight = height * (CELL_SIZE + MARGIN) + MARGIN + BORDER_WIDTH * 2
        var windowWidth = fieldWidth + INFO_SPACE_WIDTH
        var windowHeight: Int
        if (platform == "iOS") {
            val gamePadHeight = (displayHeight * windowWidth - fieldHeight * displayWidth) / displayWidth
            windowHeight = fieldHeight + gamePadHeight
            gamePadButtons = GamePadButtons(windowWidth, windowHeight, gamePadHeight)
            windowX = 0
            windowY = 0
            ratio = displayHeight.toFloat() / windowHeight
            windowWidth = displayWidth
            windowHeight = displayHeight
        } else {
            windowHeight = fieldHeight
            gamePadButtons = null
            windowX = (displayWidth - windowWidth) / 2
            windowY = (displayHeight - windowHeight) / 2
            ratio = 1.0f
        }
        val window = SDL_CreateWindow("Tetris", windowX, windowY, windowWidth, windowHeight, SDL_WINDOW_SHOWN)
        if (window == null) {
            println("SDL_CreateWindow Error: ${get_SDL_Error()}")
            SDL_Quit()
            throw Error()
        }
        this.window = window

        val renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED or SDL_RENDERER_PRESENTVSYNC)
        if (renderer == null) {
            SDL_DestroyWindow(window)
            println("SDL_CreateRenderer Error: ${get_SDL_Error()}")
            SDL_Quit()
            throw Error()
        }
        this.renderer = renderer

        texture = loadImage(window, renderer, "tetris_all.bmp")
    }

    private fun loadImage(win: CPointer<SDL_Window>, ren: CPointer<SDL_Renderer>, imagePath: String): CPointer<SDL_Texture> {
        val bmp = SDL_LoadBMP_RW(SDL_RWFromFile(imagePath, "rb"), 1);
        if (bmp == null) {
            SDL_DestroyRenderer(ren)
            SDL_DestroyWindow(win)
            println("SDL_LoadBMP_RW Error: ${get_SDL_Error()}")
            SDL_Quit()
            throw Error()
        }

        val tex = SDL_CreateTextureFromSurface(ren, bmp)
        SDL_FreeSurface(bmp)
        if (tex == null) {
            SDL_DestroyRenderer(ren)
            SDL_DestroyWindow(win)
            println("SDL_CreateTextureFromSurface Error: ${get_SDL_Error()}")
            SDL_Quit()
            throw Error()
        }
        return tex
    }

    override fun drawCell(x: Int, y: Int, cell: Byte) {
        field[x][y] = cell
    }

    override fun drawNextPieceCell(x: Int, y: Int, cell: Byte) {
        nextPieceField[x][y] = cell
    }

    override fun setInfo(linesCleared: Int, level: Int, score: Int, tetrises: Int) {
        this.linesCleared = linesCleared
        this.level = level
        this.score = score
        this.tetrises = tetrises
    }

    override fun refresh() {
        SDL_RenderClear(renderer)
        drawField()
        drawInfo()
        drawNextPiece()
        drawGamePad()
        SDL_RenderPresent(renderer)
    }

    private fun drawBorder(topLeftX: Int, topLeftY: Int, width: Int, height: Int) {
        // Upper-left corner.
        var srcX = CELLS_WIDTH
        var srcY = 0
        var destX = topLeftX
        var destY = topLeftY
        copyRect(srcX, srcY, destX, destY, BORDER_WIDTH + MARGIN, BORDER_WIDTH)

        // Upper margin.
        srcX += BORDER_WIDTH + MARGIN
        destX += BORDER_WIDTH + MARGIN
        for (i in 0..width - 1) {
            copyRect(srcX, srcY, destX, destY, CELL_SIZE + MARGIN, BORDER_WIDTH)
            destX += CELL_SIZE + MARGIN
        }

        // Upper-right corner.
        srcX += CELL_SIZE + MARGIN
        copyRect(srcX, srcY, destX, destY, BORDER_WIDTH, BORDER_WIDTH + MARGIN)

        // Right margin.
        srcY += BORDER_WIDTH + MARGIN
        destY += BORDER_WIDTH + MARGIN
        for (j in 0..height - 1) {
            copyRect(srcX, srcY, destX, destY, BORDER_WIDTH, CELL_SIZE + MARGIN)
            destY += CELL_SIZE + MARGIN
        }

        // Left margin.
        srcX = CELLS_WIDTH
        srcY = BORDER_WIDTH
        destX = topLeftX
        destY = topLeftY + BORDER_WIDTH
        for (j in 0..height - 1) {
            copyRect(srcX, srcY, destX, destY, BORDER_WIDTH, CELL_SIZE + MARGIN)
            destY += CELL_SIZE + MARGIN
        }

        // Left-down corner.
        srcY += CELL_SIZE + MARGIN
        copyRect(srcX, srcY, destX, destY, BORDER_WIDTH, BORDER_WIDTH + MARGIN)

        // Down marign.
        srcX += BORDER_WIDTH
        srcY += MARGIN
        destX += BORDER_WIDTH
        destY += MARGIN
        for (i in 0..width - 1) {
            copyRect(srcX, srcY, destX, destY, CELL_SIZE + MARGIN, BORDER_WIDTH)
            destX += CELL_SIZE + MARGIN

        }
        // Right-down corner.
        srcX += CELL_SIZE + MARGIN
        copyRect(srcX, srcY, destX, destY, BORDER_WIDTH + MARGIN, BORDER_WIDTH)
    }

    private fun drawField() {
        drawField(field    = field,
                  topLeftX = 0,
                  topLeftY = 0,
                  width    = width,
                  height   = height)
    }

    private fun drawNextPiece() {
        drawInt(labelSrcX   = LEVEL_LABEL_WIDTH,
                labelSrcY   = CELLS_HEIGHT + SYMBOL_SIZE,
                labelDestX  = fieldWidth + SYMBOL_SIZE,
                labelDestY  = getInfoY(5),
                labelWidth  = NEXT_LABEL_WIDTH,
                totalDigits = 0,
                value       = 0)
        drawField(field    = nextPieceField,
                  topLeftX = fieldWidth + SYMBOL_SIZE,
                  topLeftY = getInfoY(6),
                  width    = 4,
                  height   = 4)
    }

    private fun drawField(field: Field, topLeftX: Int, topLeftY: Int, width: Int, height: Int) {
        drawBorder(topLeftX = topLeftX,
                   topLeftY = topLeftY,
                   width    = width,
                   height   = height)
        for (i in 0..height - 1)
            for (j in 0..width - 1) {
                val cell = field[i][j].toInt()
                if (cell == 0) continue
                copyRect(srcX  = (level % COLORS) * CELL_SIZE,
                         srcY  = (3 - cell) * CELL_SIZE,
                         destX = topLeftX + BORDER_WIDTH + MARGIN + j * (CELL_SIZE + MARGIN),
                         destY = topLeftY + BORDER_WIDTH + MARGIN + i * (CELL_SIZE + MARGIN),
                         width = CELL_SIZE,
                         height = CELL_SIZE)
            }
    }

    private fun drawInfo() {
        drawInt(labelSrcX   = LINES_LABEL_WIDTH,
                labelSrcY   = CELLS_HEIGHT,
                labelDestX  = fieldWidth + SYMBOL_SIZE,
                labelDestY  = getInfoY(0),
                labelWidth  = SCORE_LABEL_WIDTH,
                totalDigits = 6,
                value       = score)
        drawInt(labelSrcX   = 0,
                labelSrcY   = CELLS_HEIGHT,
                labelDestX  = fieldWidth + SYMBOL_SIZE,
                labelDestY  = getInfoY(1),
                labelWidth  = LINES_LABEL_WIDTH,
                totalDigits = 3,
                value       = linesCleared)
        drawInt(labelSrcX   = 0,
                labelSrcY   = CELLS_HEIGHT + SYMBOL_SIZE,
                labelDestX  = fieldWidth + SYMBOL_SIZE,
                labelDestY  = getInfoY(2),
                labelWidth  = LEVEL_LABEL_WIDTH,
                totalDigits = 2,
                value       = level)
        drawInt(labelSrcX   = 0,
                labelSrcY   = CELLS_HEIGHT + SYMBOL_SIZE * 2,
                labelDestX  = fieldWidth + SYMBOL_SIZE,
                labelDestY  = getInfoY(3),
                labelWidth  = TETRISES_LABEL_WIDTH,
                totalDigits = 2,
                value       = tetrises)
    }

    private fun getInfoY(line: Int): Int {
        return SYMBOL_SIZE * (2 * line + 1) + INFO_MARGIN * line
    }

    private fun drawInt(labelSrcX: Int, labelSrcY: Int, labelDestX: Int, labelDestY: Int,
                        labelWidth: Int, totalDigits: Int, value: Int) {
        copyRect(srcX   = labelSrcX,
                 srcY   = labelSrcY,
                 destX  = labelDestX,
                 destY  = labelDestY,
                 width  = labelWidth,
                 height = SYMBOL_SIZE)
        val digits = IntArray(totalDigits)
        var x = value
        for (i in 0..totalDigits - 1) {
            digits[totalDigits - 1 - i] = x % 10
            x = x / 10
        }
        for (i in 0..totalDigits - 1) {
            copyRect(srcX   = digits[i] * SYMBOL_SIZE,
                     srcY   = CELLS_HEIGHT + 3 * SYMBOL_SIZE,
                     destX  = labelDestX + SYMBOL_SIZE + i * SYMBOL_SIZE,
                     destY  = labelDestY + SYMBOL_SIZE,
                     width  = SYMBOL_SIZE,
                     height = SYMBOL_SIZE)
        }
    }

    private fun drawGamePad() {
        if (gamePadButtons == null) return
        SDL_SetRenderDrawColor(renderer, 127, 127, 127, SDL_ALPHA_OPAQUE.toByte())
        fillRect(gamePadButtons.leftRect)
        fillRect(gamePadButtons.downRect)
        fillRect(gamePadButtons.dropRect)
        fillRect(gamePadButtons.rightRect)
        fillRect(gamePadButtons.rotateRect)
        SDL_SetRenderDrawColor(renderer, 0, 0, 0, SDL_ALPHA_OPAQUE.toByte())
    }

    private fun fillRect(rect: SDL_Rect) {
        memScoped {
            val stretchedRect = alloc<SDL_Rect>()
            stretchedRect.w = stretch(rect.w)
            stretchedRect.h = stretch(rect.h)
            stretchedRect.x = stretch(rect.x)
            stretchedRect.y = stretch(rect.y)
            SDL_RenderFillRect(renderer, stretchedRect.ptr.reinterpret())
        }
    }

    private fun copyRect(srcX: Int, srcY: Int, destX: Int, destY: Int, width: Int, height: Int) {
        memScoped {
            val srcRect = alloc<SDL_Rect>()
            val destRect = alloc<SDL_Rect>()
            srcRect.w = width
            srcRect.h = height
            srcRect.x = srcX
            srcRect.y = srcY
            destRect.w = stretch(width)
            destRect.h = stretch(height)
            destRect.x = stretch(destX)
            destRect.y = stretch(destY)
            SDL_RenderCopy(renderer, texture, srcRect.ptr.reinterpret(), destRect.ptr.reinterpret())
        }
    }

    override fun readCommands(): List<UserCommand> {
        val commands = mutableListOf<UserCommand>()
        memScoped {
            val event = alloc<SDL_Event>()
            while (SDL_PollEvent(event.ptr.reinterpret()) != 0) {
                val eventType = event.type
                when (eventType) {
                    SDL_QUIT -> commands.add(UserCommand.EXIT)
                    SDL_KEYDOWN -> {
                        val keyboardEvent = event.ptr.reinterpret<SDL_KeyboardEvent>().pointed
                        when (keyboardEvent.keysym.scancode) {
                            SDL_SCANCODE_LEFT -> commands.add(UserCommand.LEFT)
                            SDL_SCANCODE_RIGHT -> commands.add(UserCommand.RIGHT)
                            SDL_SCANCODE_DOWN -> commands.add(UserCommand.DOWN)
                            SDL_SCANCODE_Z, SDL_SCANCODE_SPACE -> commands.add(UserCommand.ROTATE)
                            SDL_SCANCODE_UP -> commands.add(UserCommand.DROP)
                            SDL_SCANCODE_ESCAPE -> commands.add(UserCommand.EXIT)
                        }
                    }
                    SDL_MOUSEBUTTONDOWN -> if (gamePadButtons != null) {
                        val mouseEvent = event.ptr.reinterpret<SDL_MouseButtonEvent>().pointed
                        val x = mouseEvent.x
                        val y = mouseEvent.y
                        val command = gamePadButtons.getCommandAt(x, y)
                        if (command != null)
                            commands.add(command)
                    }
                }
            }
        }
        return commands
    }

    fun destroy() {
        SDL_DestroyTexture(texture)
        SDL_DestroyRenderer(renderer)
        SDL_DestroyWindow(window)
        SDL_Quit()
        gamePadButtons?.destroy()
    }
}

fun main(args: Array<String>) {
    var startLevel = 0
    var width = 10
    var height = 20
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
