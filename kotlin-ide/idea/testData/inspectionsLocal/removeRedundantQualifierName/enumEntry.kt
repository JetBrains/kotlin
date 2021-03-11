// WITH_RUNTIME
import Encoding.MJPEG

class Player {
    val status: String = <caret>Encoding.MJPEG.toString()
}

enum class Encoding {
    UNKNOWN,
    MJPEG,
    H264
}