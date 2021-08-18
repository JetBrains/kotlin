import kotlinx.cinterop.useContents
import platform.posix.stat
import withPosix.getFileStat

fun main() {
    repeat(10) {
        getFileStat().useContents { score(this) }
    }
}
expect fun score(stat: stat): Int
