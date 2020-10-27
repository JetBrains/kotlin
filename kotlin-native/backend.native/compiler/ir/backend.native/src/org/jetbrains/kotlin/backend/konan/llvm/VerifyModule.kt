package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import java.io.File

internal fun verifyModule(llvmModule: LLVMModuleRef, current: String = "") = memScoped {
    val errorRef = allocPointerTo<ByteVar>()
    errorRef.value = null

    val verificationResult = LLVMVerifyModule(
            llvmModule,
            LLVMVerifierFailureAction.LLVMReturnStatusAction,
            errorRef.ptr
    )

    val verificationError = convertAndDisposeLlvmMessage(errorRef.value)

    if (verificationResult != 0) {
        throwModuleVerificationError(llvmModule, current, verificationError)
    }
}

private fun throwModuleVerificationError(
        llvmModule: LLVMModuleRef,
        current: String,
        verificationError: String?
): Nothing {
    val exceptionMessage = buildString {
        try {
            appendModuleVerificationFailureDetails(llvmModule, current, verificationError)
        } catch (e: Throwable) {
            appendLine("Failed to make full error message: ${e.message}")
        }
    }

    throw Error(exceptionMessage)
}

private fun StringBuilder.appendModuleVerificationFailureDetails(
        llvmModule: LLVMModuleRef,
        current: String,
        verificationError: String?
) {
    appendLine("Invalid LLVM module")

    if (current.isNotEmpty())
        appendLine("Error in $current")

    appendVerificationError(verificationError)

    val moduleDumpFile = createTempFile("kotlin_native_llvm_module_dump", ".ll")

    dumpModuleAndAppendDetails(llvmModule, moduleDumpFile)

    moduleDumpFile.appendText(verificationError.orEmpty())
}

private fun StringBuilder.appendVerificationError(error: String?) {
    if (error == null) return

    val lines = error.lines()
    appendLine("Verification errors:")

    val maxLines = 12

    lines.take(maxLines).forEach {
        appendLine("    $it")
    }

    if (lines.size > maxLines) {
        appendLine("    ... (full error is available at the LLVM module dump file)")
    }
}

private fun StringBuilder.dumpModuleAndAppendDetails(llvmModule: LLVMModuleRef, moduleDumpFile: File) = memScoped {
    val errorRef = allocPointerTo<ByteVar>()
    errorRef.value = null
    val printedWithErrors = LLVMPrintModuleToFile(llvmModule, moduleDumpFile.absolutePath, errorRef.ptr)

    appendLine()
    appendLine("LLVM module dump: ${moduleDumpFile.absolutePath}")

    val modulePrintError = convertAndDisposeLlvmMessage(errorRef.value)
    if (printedWithErrors != 0) {
        appendLine("  (printed with errors: $modulePrintError)")
    }
}

private fun convertAndDisposeLlvmMessage(message: CPointer<ByteVar>?): String? =
        try {
            message?.toKString()
        } finally {
            LLVMDisposeMessage(message)
        }
