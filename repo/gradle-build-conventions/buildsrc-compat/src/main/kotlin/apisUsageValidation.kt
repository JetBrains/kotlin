import org.codehaus.mojo.animal_sniffer.SignatureBuilder
import org.codehaus.mojo.animal_sniffer.SignatureChecker
import org.codehaus.mojo.animal_sniffer.logging.PrintWriterLogger
import org.codehaus.mojo.animal_sniffer.logging.Logger
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


fun Project.validateRuntimeAPIsUsage(
    oldApis: FileCollection,
    newApis: FileCollection,
    filesToValidate: FileCollection,
    sourcesPath: File,
    unsafeApisUsageAnnotationFqn: String,
): TaskProvider<*> {
    return tasks.register("apiUsageValidation") {
        inputs.files(
            oldApis,
            newApis,
            filesToValidate,
        )
        val layout = project.layout
        doLast {
            val unrecognizedSelectorsInOldApis = dumpUnrecognizedSignature(
                createKnownSignatureFile(
                    layout,
                    oldApis,
                    "oldApis",
                ),
                filesToValidate,
                sourcesPath,
                unsafeApisUsageAnnotationFqn
            )
            val unrecognizedSelectorsInNewApis = dumpUnrecognizedSignature(
                createKnownSignatureFile(
                    layout,
                    newApis,
                    "newApis",
                ),
                filesToValidate,
                sourcesPath,
                unsafeApisUsageAnnotationFqn
            )

            val apisFromNewVersionNotPresentInOld = unrecognizedSelectorsInOldApis.subtract(unrecognizedSelectorsInNewApis)
            val apisFromOldVersionNotPresentInNew = unrecognizedSelectorsInNewApis.subtract(unrecognizedSelectorsInOldApis)

            if (apisFromNewVersionNotPresentInOld.isNotEmpty() || apisFromOldVersionNotPresentInNew.isNotEmpty()) {
                error(
                    buildString {
                        appendLine("Unsafe APIs unmarked by $unsafeApisUsageAnnotationFqn detected.")
                        if (apisFromNewVersionNotPresentInOld.isNotEmpty()) {
                            appendLine("These APIs only exist in newer libraries:")
                            apisFromNewVersionNotPresentInOld.forEach(::appendLine)
                            appendLine()
                        }
                        if (apisFromOldVersionNotPresentInNew.isNotEmpty()) {
                            appendLine("These APIs only exist in older libraries:")
                            apisFromOldVersionNotPresentInNew.forEach(::appendLine)
                        }
                    }
                )
            }
        }
    }
}

private fun createKnownSignatureFile(
    layout: ProjectLayout,
    apis: FileCollection,
    signatureFileName: String,
): File {
    val file = layout.buildDirectory.file(signatureFileName).get().asFile
    FileOutputStream(file).use {
        val builder = SignatureBuilder(it, PrintWriterLogger(System.out))
        apis.forEach {
            builder.process(it)
        }
        builder.close()
    }
    return file
}

private fun dumpUnrecognizedSignature(
    knownSignatures: File,
    classFiles: FileCollection,
    sourcesPath: File,
    unsafeApisUsageAnnotationFqn: String,
): Set<String> {
    val messages = mutableSetOf<String>()
    val checker = SignatureChecker(
        FileInputStream(knownSignatures), emptySet(),
        object : Logger by PrintWriterLogger(System.out) {
            override fun error(message: String?) {
                message?.let { messages.add(it) }
            }

            override fun error(message: String?, t: Throwable?) {
                message?.let { messages.add(it) }
            }
        }
    )
    checker.setAnnotationTypes(listOf(unsafeApisUsageAnnotationFqn))
    checker.setSourcePath(listOf(sourcesPath))
    classFiles.forEach {
        checker.process(it)
    }
    return messages
}