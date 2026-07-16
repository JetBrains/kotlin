package test

abstract class CrashMe2(value: Long) {
    val crashMe2 = object : Any() {
        // empty
    }
}

