import platform.posix.stat

actual fun score(stat: stat): Int {
    return stat.st_blocks.toInt()
}
