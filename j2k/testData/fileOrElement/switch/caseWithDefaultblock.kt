fun foo() {
    when (status) {
        "init", "dial", "transmit" -> return Color.BLACK

        "ok" -> return 0xFF006600.toInt()

        "cancel" -> return 0xFF666666.toInt()

        "fail", "busy", "error" -> return 0xFF660000.toInt()
        else -> return 0xFF660000.toInt()
    }
}
