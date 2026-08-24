package producer

expect fun platformName(): String

fun greeting(): String = "Hello from ${platformName()}"
