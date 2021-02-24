package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.util.*

internal class LocationReportingTestMessageCollector : MessageCollector {
    val errors = ArrayList<String>()

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        if (severity.isError) {
            errors.add("e: $location: $message")
        }
    }

    override fun clear() {
        errors.clear()
    }

    override fun hasErrors(): Boolean =
        errors.isNotEmpty()
}