package test

class CrashMe {
    private val crashMe = object : CrashMe2(1000) {
        // empty
    }
}
