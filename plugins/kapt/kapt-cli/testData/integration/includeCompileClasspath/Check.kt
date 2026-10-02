import java.io.File

fun main() {
    val enabledOutput =
        File("output/enabled/sources/generated/Test.java").isFile

    val disabledOutput =
        File("output/disabled/sources/generated/Test.java").isFile

    println("enabled: " + if (enabledOutput) "generated" else "absent")
    println("disabled: " + if (disabledOutput) "generated" else "absent")
}
