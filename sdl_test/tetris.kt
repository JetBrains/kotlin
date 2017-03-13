import kotlinx.cinterop.*
import konan.internal.*
import sdl.*

typealias Field = Array<ByteArray>

enum class Move {
    Left,
    Right,
    Down,
    Rotate
}

enum class PlacementResult(val linesCleared: Int = 0, val bonus: Int = 0) {
    Nothing,
    GameOver,
    // For values of bonuses see https://tetris.wiki/Scoring
    Single(1, 40),
    Double(2, 100),
    Triple(3, 300),
    Tetris(4, 1200)
}

val EMPTY: Byte = 0
val BRICK: Byte = -1

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
            Move.Left -> --p.y
            Move.Right -> ++p.y
            Move.Down -> ++p.x
            Move.Rotate -> state = (state + 1) % numberOfStates
        }
    }

    fun unMakeMove(move: Move) {
        when (move) {
            Move.Left -> ++p.y
            Move.Right -> --p.y
            Move.Down -> --p.x
            Move.Rotate -> state = (state + numberOfStates - 1) % numberOfStates
        }
    }
}

/*
 * We use Nintendo Rotation System, right-handed version.
 * See https://tetris.wiki/Nintendo_Rotation_System
 */
enum class Piece(private val origin_: Point, vararg val states: Field) {
    T(Point(-1, -2),
            arrayOf(
                    byteArrayOf(0, 0, 0),
                    byteArrayOf(1, 1, 1),
                    byteArrayOf(0, 1, 0)),
            arrayOf(
                    byteArrayOf(0, 1, 0),
                    byteArrayOf(1, 1, 0),
                    byteArrayOf(0, 1, 0)),
            arrayOf(
                    byteArrayOf(0, 1, 0),
                    byteArrayOf(1, 1, 1),
                    byteArrayOf(0, 0, 0)),
            arrayOf(
                    byteArrayOf(0, 1, 0),
                    byteArrayOf(0, 1, 1),
                    byteArrayOf(0, 1, 0))
    ),
    J(Point(-1, -2),
            arrayOf(
                    byteArrayOf(0, 0, 0),
                    byteArrayOf(2, 2, 2),
                    byteArrayOf(0, 0, 2)),
            arrayOf(
                    byteArrayOf(0, 2, 0),
                    byteArrayOf(0, 2, 0),
                    byteArrayOf(2, 2, 0)),
            arrayOf(
                    byteArrayOf(2, 0, 0),
                    byteArrayOf(2, 2, 2),
                    byteArrayOf(0, 0, 0)),
            arrayOf(
                    byteArrayOf(0, 2, 2),
                    byteArrayOf(0, 2, 0),
                    byteArrayOf(0, 2, 0))
    ),
    Z(Point(-1, -2),
            arrayOf(
                    byteArrayOf(0, 0, 0),
                    byteArrayOf(3, 3, 0),
                    byteArrayOf(0, 3, 3)),
            arrayOf(
                    byteArrayOf(0, 0, 3),
                    byteArrayOf(0, 3, 3),
                    byteArrayOf(0, 3, 0))
    ),
    O(Point(0, -2),
            arrayOf(
                    byteArrayOf(1, 1),
                    byteArrayOf(1, 1))
    ),
    S(Point(-1, -2),
            arrayOf(
                    byteArrayOf(0, 0, 0),
                    byteArrayOf(0, 2, 2),
                    byteArrayOf(2, 2, 0)),
            arrayOf(
                    byteArrayOf(0, 2, 0),
                    byteArrayOf(0, 2, 2),
                    byteArrayOf(0, 0, 2))
    ),
    L(Point(-1, -2),
            arrayOf(
                    byteArrayOf(0, 0, 0),
                    byteArrayOf(3, 3, 3),
                    byteArrayOf(3, 0, 0)),
            arrayOf(
                    byteArrayOf(3, 3, 0),
                    byteArrayOf(0, 3, 0),
                    byteArrayOf(0, 3, 0)),
            arrayOf(
                    byteArrayOf(0, 0, 3),
                    byteArrayOf(3, 3, 3),
                    byteArrayOf(0, 0, 0)),
            arrayOf(
                    byteArrayOf(0, 3, 0),
                    byteArrayOf(0, 3, 0),
                    byteArrayOf(0, 3, 3))
    ),
    I(Point(-2, -2),
            arrayOf(
                    byteArrayOf(0, 0, 0, 0),
                    byteArrayOf(0, 0, 0, 0),
                    byteArrayOf(1, 1, 1, 1),
                    byteArrayOf(0, 0, 0, 0)),
            arrayOf(
                    byteArrayOf(0, 0, 1, 0),
                    byteArrayOf(0, 0, 1, 0),
                    byteArrayOf(0, 0, 1, 0),
                    byteArrayOf(0, 0, 1, 0))
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
    RELEASE,
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
    val MARGIN = 4

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
        if (isOutOfBorders()) return PlacementResult.GameOver
        switchCurrentPiece()
        if (!currentPiece.canBePlaced(field, currentPosition))
            return PlacementResult.GameOver
        when (linesCleared) {
            1 -> return PlacementResult.Single
            2 -> return PlacementResult.Double
            3 -> return PlacementResult.Triple
            4 -> return PlacementResult.Tetris
            else -> return PlacementResult.Nothing
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
        println(placementResult.toString())
        when (placementResult) {
            PlacementResult.Nothing -> return
            PlacementResult.GameOver -> {
                gameOver = true
                return
            }
            else -> {
                linesCleared += placementResult.linesCleared
                linesClearedAtCurrentLevel += placementResult.linesCleared
                score += placementResult.bonus * (level + 1)
                if (placementResult == PlacementResult.Tetris)
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
        var prevCmd: UserCommand? = null
        var das = false
        var dasTicks = 0
        var attemptsToLock = 0
        while (!gameOver) {
            sleep(1000 / 60) // Refresh rate - 60 frames per second.
            //sleep(200)
            val commands = userInput.readCommands()
            for (cmd in commands) {
                val success: Boolean
                when (cmd) {
                    UserCommand.EXIT -> return
                    UserCommand.LEFT -> success = field.makeMove(Move.Left)
                    UserCommand.RIGHT -> success = field.makeMove(Move.Right)
                    UserCommand.ROTATE -> success = field.makeMove(Move.Rotate)
                    UserCommand.DOWN -> {
                        success = field.makeMove(Move.Down)
                        if (!success) placePiece()
                    }
                    UserCommand.RELEASE -> {
                        while (field.makeMove(Move.Down)) {
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
            if (!field.makeMove(Move.Down)) {
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

fun main(args: Array<String>) {
    SDL_main(args)
}

fun print_SDL_Error() {
    externals.printf(SDL_GetError()!!.rawValue)
}

class SDL_Visualizer(val width: Int, val height: Int): GameFieldVisualizer, UserInput {
    private val CELL_SIZE = 20
    private val SYMBOL_SIZE = 21
    private val INFO_MARGIN = 10
    private val MARGIN = 2
    private val BORDER_WIDTH = 18
    private val INFO_SPACE_WIDTH = SYMBOL_SIZE * (2 + 8)

    private val field: Field = Array<ByteArray>(height) { ByteArray(width) }
    private val nextPieceField: Field = Array<ByteArray>(4) { ByteArray(4) }
    private var linesCleared: Int = 0
    private var level: Int = 0
    private var score: Int = 0
    private var tetrises: Int = 0

    private val fieldWidth: Int
    private val fieldHeight: Int
    private val window: CPointer<SDL_Window>
    private val renderer: CPointer<SDL_Renderer>
    private val cells: CPointer<SDL_Texture>
    private val texts: CPointer<SDL_Texture>

    init {
        if (SDL_Init(SDL_INIT_EVERYTHING) != 0) {
            //println("SDL_Init error: ${SDL_GetError()}")
            print("SDL_CreateWindow Error: ")
            print_SDL_Error()
            println()
            throw Error()
        }

        fieldWidth = width * (CELL_SIZE + MARGIN) + MARGIN + BORDER_WIDTH * 2
        fieldHeight = height * (CELL_SIZE + MARGIN) + MARGIN + BORDER_WIDTH * 2
        val win = SDL_CreateWindow("Tetris", 100, 100, fieldWidth + INFO_SPACE_WIDTH,
                fieldHeight, SDL_WindowFlags.SDL_WINDOW_SHOWN.value)
        if (win == null) {
            //println("SDL_CreateWindow Error: ${SDL_GetError()!!.asCString()}")
            print("SDL_CreateWindow Error: ")
            print_SDL_Error()
            println()
            SDL_Quit()
            throw Error()
        }
        window = win

        val ren = SDL_CreateRenderer(win, -1, (SDL_RendererFlags.SDL_RENDERER_ACCELERATED or SDL_RendererFlags.SDL_RENDERER_PRESENTVSYNC).value)
        if (ren == null) {
            SDL_DestroyWindow(win)
            //println("SDL_CreateRenderer Error: ${SDL_GetError()!!.asCString()}")
            println("SDL_CreateRenderer Error: ")
            print_SDL_Error()
            println()
            SDL_Quit()
            throw Error()
        }
        renderer = ren

        // TODO: merge together.
        cells = loadImage(win, ren, "tetris_all.bmp")
        texts = loadImage(win, ren, "tetris_cells_texts.bmp")
//        if (SDL_SetTextureAlphaMod(tex, 63) != 0) {
//            println("Unable to set alpha mod")
//            print_SDL_Error()
//            println()
//        }
//        if (SDL_SetTextureColorMod(tex, 127, 127, 127) != 0) {
//            println("Unable to set color mod")
//            print_SDL_Error()
//            println()
//        }
    }

    private fun loadImage(win: CPointer<SDL_Window>, ren: CPointer<SDL_Renderer>, imagePath: String): CPointer<SDL_Texture> {
        val bmp = SDL_LoadBMP_RW(SDL_RWFromFile(imagePath, "rb"), 1);
        if (bmp == null) {
            SDL_DestroyRenderer(ren)
            SDL_DestroyWindow(win)
            //println("SDL_LoadBMP Error: ${SDL_GetError()!!.asCString()}")
            println("SDL_LoadBMP Error: ")
            print_SDL_Error()
            println()
            SDL_Quit()
            throw Error()
        }

        val tex = SDL_CreateTextureFromSurface(ren, bmp)
        SDL_FreeSurface(bmp)
        if (tex == null) {
            SDL_DestroyRenderer(ren)
            SDL_DestroyWindow(win)
            //println("SDL_CreateTextureFromSurface Error: ${SDL_GetError()!!.asCString()}")
            println("SDL_CreateTextureFromSurface Error: ")
            print_SDL_Error()
            println()
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
        drawBorder()
        drawField()
        drawInfo()
        drawNextPiece()
        SDL_RenderPresent(renderer)
    }

    private fun drawBorder() {
        memScoped {
            val rect = alloc<SDL_Rect>()
            rect.w.value = fieldWidth
            rect.h.value = fieldHeight
            rect.x.value = 0
            rect.y.value = 0
            SDL_SetRenderDrawColor(renderer, -128, 0, -128, -128)
            SDL_RenderFillRect(renderer, rect.ptr.reinterpret())
            rect.w.value = fieldWidth - BORDER_WIDTH * 2
            rect.h.value = fieldHeight - BORDER_WIDTH * 2
            rect.x.value = BORDER_WIDTH
            rect.y.value = BORDER_WIDTH
            SDL_SetRenderDrawColor(renderer, 0, 0, 0, -128)
            SDL_RenderFillRect(renderer, rect.ptr.reinterpret())
        }
    }

    private fun drawField() {
        drawField(field, width, height, MARGIN + BORDER_WIDTH, MARGIN + BORDER_WIDTH)
    }

    private fun drawNextPiece() {
        drawInt(103, 60 + SYMBOL_SIZE, fieldWidth + SYMBOL_SIZE, SYMBOL_SIZE * 11 + INFO_MARGIN * 6, 85, 0, 0)
        drawField(nextPieceField, 4, 4,
                fieldWidth + SYMBOL_SIZE + MARGIN + CELL_SIZE + MARGIN,
                SYMBOL_SIZE * 12 + INFO_MARGIN * 7 + MARGIN)
    }

    private fun drawField(f: Field, w: Int, h: Int, x: Int, y: Int) {
        memScoped {
            val srcRect = alloc<SDL_Rect>()
            val destRect = alloc<SDL_Rect>()
            srcRect.w.value = CELL_SIZE
            srcRect.h.value = CELL_SIZE
            destRect.w.value = CELL_SIZE
            destRect.h.value = CELL_SIZE
            for (i in 0..h - 1)
                for (j in 0..w - 1) {
                    val cell = f[i][j].toInt()
                    if (cell == 0) continue
                    srcRect.x.value = (cell - 1) * CELL_SIZE
                    srcRect.y.value = (level % 10) * CELL_SIZE
                    destRect.x.value = x + j * (CELL_SIZE + MARGIN)
                    destRect.y.value = y + i * (CELL_SIZE + MARGIN)
                    SDL_RenderCopy(renderer, cells, srcRect.ptr.reinterpret(), destRect.ptr.reinterpret())
                }
        }
    }

    private fun drawInfo() {
        drawInt(104, 60, fieldWidth + SYMBOL_SIZE, SYMBOL_SIZE, 107, 6, score)
        drawInt(0, 60, fieldWidth + SYMBOL_SIZE, SYMBOL_SIZE * 3 + INFO_MARGIN, 104, 3, linesCleared)
        drawInt(0, 60 + SYMBOL_SIZE, fieldWidth + SYMBOL_SIZE, SYMBOL_SIZE * 5 + INFO_MARGIN * 2, 103, 2, level)
        drawInt(0, 60 + SYMBOL_SIZE * 2, fieldWidth + SYMBOL_SIZE, SYMBOL_SIZE * 7 + INFO_MARGIN * 3, 162, 2, tetrises)
    }

    private fun drawInt(labelSrcX: Int, labelSrcY: Int, labelDestX: Int, labelDestY: Int,
                        labelWidth: Int, totalDigits: Int, value: Int) {
        memScoped {
            val srcRect = alloc<SDL_Rect>()
            val destRect = alloc<SDL_Rect>()

            srcRect.w.value = labelWidth
            srcRect.h.value = SYMBOL_SIZE
            srcRect.x.value = labelSrcX
            srcRect.y.value = labelSrcY
            destRect.w.value = labelWidth
            destRect.h.value = SYMBOL_SIZE
            destRect.x.value = labelDestX
            destRect.y.value = labelDestY

            SDL_RenderCopy(renderer, texts, srcRect.ptr.reinterpret(), destRect.ptr.reinterpret())

            val digits = IntArray(totalDigits)
            var x = value
            for (i in 0..totalDigits - 1) {
                digits[totalDigits - 1 - i] = x % 10
                x = x / 10
            }
            srcRect.w.value = SYMBOL_SIZE
            srcRect.h.value = SYMBOL_SIZE
            destRect.w.value = SYMBOL_SIZE
            destRect.h.value = SYMBOL_SIZE
            for (i in 0..totalDigits - 1) {
                srcRect.x.value = digits[i] * SYMBOL_SIZE
                srcRect.y.value = 123 // TODO: constant.
                destRect.x.value = labelDestX + SYMBOL_SIZE + i * SYMBOL_SIZE
                destRect.y.value = labelDestY + SYMBOL_SIZE
                SDL_RenderCopy(renderer, texts, srcRect.ptr.reinterpret(), destRect.ptr.reinterpret())
            }
        }
    }

    private val LEFT_PRESSED = 1
    private val RIGHT_PRESSED = 2
    private val DOWN_PRESSED = 4
    private val UP_PRESSED = 8
    private val Z_PRESSED = 16
    private val ESC_PRESSED = 32
    private var pressedKeys = 0
    private var stickedKeys = 0

    private fun keyToCommand(key: Int): UserCommand? =
        when (key) {
            LEFT_PRESSED -> (UserCommand.LEFT)
            RIGHT_PRESSED -> (UserCommand.RIGHT)
            DOWN_PRESSED -> (UserCommand.DOWN)
            Z_PRESSED -> (UserCommand.ROTATE)
            UP_PRESSED -> (UserCommand.RELEASE)
            ESC_PRESSED -> (UserCommand.EXIT)
            else -> null
        }

    override fun readCommands(): List<UserCommand> {
        val commands = mutableListOf<UserCommand>()
        memScoped {
            val event = alloc<SDL_Event>()
            var currentCommands = 0
            while (SDL_PollEvent(event.ptr.reinterpret()) != 0) {
                if (event.type.value == SDL_EventType.SDL_KEYDOWN.value
                        || event.type.value == SDL_EventType.SDL_KEYUP.value) {
                    val keyboardEvent = event.ptr.reinterpret<SDL_KeyboardEvent>().pointed
                    val scancode = when (keyboardEvent.keysym.scancode.value) {
                        SDL_Scancode.SDL_SCANCODE_LEFT -> LEFT_PRESSED
                        SDL_Scancode.SDL_SCANCODE_RIGHT -> RIGHT_PRESSED
                        SDL_Scancode.SDL_SCANCODE_DOWN -> DOWN_PRESSED
                        SDL_Scancode.SDL_SCANCODE_Z -> Z_PRESSED
                        SDL_Scancode.SDL_SCANCODE_UP -> UP_PRESSED
                        SDL_Scancode.SDL_SCANCODE_ESCAPE -> ESC_PRESSED
                        else -> 0
                    }
                    if (keyboardEvent.state.value.toInt() == SDL_PRESSED) {
                        if (pressedKeys and scancode != 0)
                            stickedKeys = stickedKeys or scancode
                        currentCommands = currentCommands or scancode
                        pressedKeys = pressedKeys or scancode
                    }
                    else if (keyboardEvent.state.value.toInt() == SDL_RELEASED) {
                        pressedKeys = pressedKeys and scancode.inv()
                        stickedKeys = stickedKeys and scancode.inv()
                    }
                }
            }
            currentCommands = currentCommands or stickedKeys
            var temp = currentCommands
            while (temp != 0) {
                val next = temp and (temp - 1)
                val curKey = temp - next
                when (curKey) {
                    LEFT_PRESSED -> commands.add(UserCommand.LEFT)
                    RIGHT_PRESSED -> commands.add(UserCommand.RIGHT)
                    DOWN_PRESSED -> commands.add(UserCommand.DOWN)
                    Z_PRESSED -> commands.add(UserCommand.ROTATE)
                    UP_PRESSED -> commands.add(UserCommand.RELEASE)
                    ESC_PRESSED -> commands.add(UserCommand.EXIT)
                }
                temp = next
            }
        }
        return commands
    }

    fun destroy() {
        SDL_DestroyTexture(cells)
        SDL_DestroyRenderer(renderer)
        SDL_DestroyWindow(window)
        SDL_Quit()
    }
}

@ExportForCppRuntime("SDL_main")
fun SDL_main(args: Array<String>) {
    var startLevel = 0
    var width = 10
    var height = 20
    when (args.size) {
        1 -> startLevel = atoi(args[0])
        2 -> {
            width = atoi(args[0])
            height = atoi(args[1])
        }
        3 -> {
            width = atoi(args[0])
            height = atoi(args[1])
            startLevel = atoi(args[2])
        }
    }
    val visualizer = SDL_Visualizer(width, height)
    val game = Game(width, height, visualizer, visualizer)
    game.startNewGame(startLevel)

    return
}