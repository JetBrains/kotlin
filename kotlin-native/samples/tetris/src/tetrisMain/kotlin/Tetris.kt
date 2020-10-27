/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.tetris

import kotlinx.cinterop.*
import platform.posix.*

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
        srand(time(null).toUInt())
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

