package common.watchos.lib

expect val bitness: Int

actual fun platform(): String = "Device"
