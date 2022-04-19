// DONT_TARGET_EXACT_BACKEND: JS

import kotlin.js.Console

external val consoleAccessCounter: Int
external fun resetConsoleAccessCounter(): Unit

inline fun resetCounter(): Unit { resetConsoleAccessCounter() }

fun logHelloWorld(c: Console) {
    c.log("Hello")
    c.log("world")
    c.log("!")
}

inline fun logHelloWorldInline(c: Console) {
    c.log("Hello")
    c.log("world")
    c.log("!")
}

fun box(): String {
    js("""
        var consoleAccessCounter = 0;
                
        function resetConsoleAccessCounter() {
            consoleAccessCounter = 0;
        }
        
        Object.defineProperty(globalThis, 'console', {
            get: function () {
                ++consoleAccessCounter;
                return { log: function () {} };
            }
        });
    """)

    logHelloWorld(console)
    if (consoleAccessCounter != 1) return "Fail 1"

    logHelloWorldInline(console)
    if (consoleAccessCounter != 2) return "Fail 2"

    console.also {
        it.log("foo")
        it.log("bar")
    }
    if (consoleAccessCounter != 3) return "Fail 3"

    console.apply {
        log("foo")
        log("bar")
    }
    if (consoleAccessCounter != 4) return "Fail 4"

    console.log(resetCounter())
    if (consoleAccessCounter != 0) return "Fail 5"

    return "OK"
}
