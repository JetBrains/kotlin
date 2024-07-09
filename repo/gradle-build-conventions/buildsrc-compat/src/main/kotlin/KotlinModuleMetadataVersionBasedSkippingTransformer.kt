/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext
import kotlinx.metadata.jvm.JvmMetadataVersion
import kotlinx.metadata.jvm.KotlinModuleMetadata
import kotlinx.metadata.jvm.UnstableMetadataApi
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.file.FileTreeElement
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import java.io.Serializable

data class KotlinMetadataPivotVersion(val major: Int, val minor: Int, val patch: Int) : Serializable {
    override fun toString(): String = "$major.$minor.$patch"
}

/**
 * Shadow JAR transformer that excludes .kotlin_module files of a version >= [pivotVersion]
 */
class KotlinModuleMetadataVersionBasedSkippingTransformer : Transformer {
    private val kotlinModules: MutableMap<String, ByteArray> = mutableMapOf()
    private val logger = Logging.getLogger(this::class.java)

    @Input
    lateinit var pivotVersion: KotlinMetadataPivotVersion // it's not possible to use the Provider API here, as shadow uses regular reflection

    private val pivotVersionAsMetadataVersion: JvmMetadataVersion by lazy {
        JvmMetadataVersion(pivotVersion.major, pivotVersion.minor, pivotVersion.patch)
    }

    override fun getName(): String = "Skips .kotlin_module files of a version >= $pivotVersionAsMetadataVersion"

    override fun canTransformResource(element: FileTreeElement): Boolean = element.path.endsWith(".kotlin_module")

    @OptIn(UnstableMetadataApi::class)
    override fun transform(context: TransformerContext) {
        val metadataBytes = context.`is`.readBytes()
        val version = KotlinModuleMetadata.read(metadataBytes).version
        if (version >= pivotVersionAsMetadataVersion) {
            logger.info("Skipping ${context.path}, because its version $version is >= than $pivotVersionAsMetadataVersion")
            return
        }
        kotlinModules[context.path] = metadataBytes
    }

    override fun hasTransformedResource(): Boolean = kotlinModules.isNotEmpty()

    override fun modifyOutputStream(os: ZipOutputStream, preserveFileTimestamps: Boolean) {
        for ((path, kotlinModule) in kotlinModules) {
            val entry = ZipEntry(path)
            entry.time = TransformerContext.getEntryTimestamp(preserveFileTimestamps, entry.time)
            os.putNextEntry(entry)
            os.write(kotlinModule)
            os.closeEntry()
        }
    }
}