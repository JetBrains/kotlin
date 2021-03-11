package org.jetbrains.kotlin.testGenerator.model

import org.jetbrains.kotlin.test.TargetBackend
import java.io.File

data class TModel(
    val path: String,
    val pattern: Regex,
    val testClassName: String,
    val testMethodName: String,
    val flatten: Boolean,
    val targetBackend: TargetBackend,
    val excludedDirectories: List<String>,
    val depth: Int
)

object Patterns {
    private fun forExtension(extension: String): Regex {
        val escapedExtension = Regex.escapeReplacement(extension)
        return "^(.+)\\.$escapedExtension\$".toRegex()
    }

    val DIRECTORY: Regex = "^([^\\.]+)$".toRegex()

    val TEST: Regex = forExtension("test")
    val KT: Regex = forExtension("kt")
    val TXT: Regex = forExtension("txt")
    val KTS: Regex = forExtension("kts")
    val JAVA: Regex = forExtension("java")
    val WS_KTS: Regex = forExtension("ws.kts")

    val KT_OR_KTS: Regex = "^(.+)\\.(kt|kts)$".toRegex()
    val KT_WITHOUT_DOTS: Regex = "^([^.]+)\\.kt$".toRegex()
    val KT_OR_KTS_WITHOUT_DOTS: Regex = "^([^.]+)\\.(kt|kts)$".toRegex()
}

fun MutableTSuite.model(
    path: String,
    pattern: Regex = Patterns.KT,
    isRecursive: Boolean = true,
    testClassName: String = File(path).toJavaIdentifier().capitalize(),
    testMethodName: String = "doTest",
    flatten: Boolean = false,
    targetBackend: TargetBackend = TargetBackend.ANY,
    excludedDirectories: List<String> = emptyList(),
    depth: Int = Int.MAX_VALUE
) {
    models += TModel(
        path, pattern, testClassName, testMethodName,
        flatten, targetBackend, excludedDirectories, if (!isRecursive) 0 else depth
    )
}

fun makeJavaIdentifier(text: String): String {
    return buildString {
        for (c in text) {
            append(if (Character.isJavaIdentifierPart(c)) c else "_")
        }
    }
}

fun File.toJavaIdentifier() = makeJavaIdentifier(nameWithoutExtension)