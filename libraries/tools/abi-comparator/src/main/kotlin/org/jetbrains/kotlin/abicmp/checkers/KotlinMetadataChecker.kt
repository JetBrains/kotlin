package org.jetbrains.kotlin.abicmp.checkers

import com.github.difflib.DiffUtils
import org.jetbrains.kotlin.abicmp.metadata.renderKotlinMetadata
import org.jetbrains.kotlin.abicmp.reports.ClassReport
import org.jetbrains.kotlin.abicmp.reports.TextDiffEntry
import org.jetbrains.org.objectweb.asm.tree.ClassNode

class KotlinMetadataChecker : ClassChecker {
    override val name = "class.metadata"

    override fun check(class1: ClassNode, class2: ClassNode, report: ClassReport) {
        val metadata1 = class1.renderKotlinMetadata() ?: ""
        val metadata2 = class2.renderKotlinMetadata() ?: ""
        if (metadata1 == metadata2) return

        val patch = DiffUtils.diff(metadata1, metadata2, null)
        for (delta in patch.deltas) {
            report.addMetadataDiff(
                TextDiffEntry(
                    delta.source.lines.toList(), delta.target.lines.toList()
                )
            )
        }
    }
}