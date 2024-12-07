// FILE: Sam.java
@SamWithReceiver
public interface Sam {
    String run(String argument);
}

// FILE: test.kt
annotation class SamWithReceiver

fun takeSam(argument: String, sam: Sam): String {
    return sam.run(argument)
}

fun box(): String {
    val sam = Sam { this }
    return takeSam("OK", sam)
}
