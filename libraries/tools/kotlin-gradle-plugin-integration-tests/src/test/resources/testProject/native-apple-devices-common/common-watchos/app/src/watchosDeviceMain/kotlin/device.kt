package common.watchos.app

expect val bitness: Int

actual fun platform(): String = "Device$bitness"