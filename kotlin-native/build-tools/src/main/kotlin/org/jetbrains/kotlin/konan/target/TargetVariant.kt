/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import org.gradle.api.Named
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeDisambiguationRule
import org.gradle.api.attributes.AttributesSchema
import org.gradle.api.attributes.MultipleCandidatesDetails
import java.io.Serializable

/**
 * [Target][KonanTarget] with optional [sanitizer][SanitizerKind].
 * With [arm64e], the arm64e variant of the target, which `-Xbinary=arm64e=true` selects, see [arm64eTargetTriple].
 *
 * Can be used as a gradle attribute: `attribute(TargetVariant.TARGET_ATTRIBUTE, target.withSanitizer())`
 */
class TargetVariant @JvmOverloads constructor(
        val target: KonanTarget,
        val sanitizer: SanitizerKind?,
        val arm64e: Boolean = false,
) : Named, Serializable {
    override fun getName(): String = "$target${sanitizer.targetSuffix}${if (arm64e) "_arm64e" else ""}"

    override fun toString(): String = name

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        val otherTarget = other as? TargetVariant ?: return false
        return name == otherTarget.name
    }

    companion object {
        @JvmField
        val TARGET_ATTRIBUTE = Attribute.of("org.jetbrains.kotlin.target", TargetVariant::class.java)

        @JvmField
        val host = TargetVariant(HostManager.host, null)
    }
}

/**
 * Construct [TargetVariant] from [target][KonanTarget] and optional [sanitizer][SanitizerKind].
 */
fun KonanTarget.withSanitizer(sanitizer: SanitizerKind? = null) = TargetVariant(this, sanitizer)

/**
 * Construct the arm64e variant of [target][KonanTarget], see [TargetVariant].
 */
fun KonanTarget.arm64eVariant() = TargetVariant(this, sanitizer = null, arm64e = true)

/**
 * All known targets with their sanitizers.
 */
val PlatformManager.allTargetsWithSanitizers
    get() = enabledTargets(this).flatMap { target ->
        listOf(target.withSanitizer()) + target.supportedSanitizers().map {
            target.withSanitizer(it)
        }
    }

private class TargetDisambiguationRule : AttributeDisambiguationRule<TargetVariant> {
    override fun execute(details: MultipleCandidatesDetails<TargetVariant>) = details.run {
        if (consumerValue == null) {
            // If the consumer didn't want a specific target, provide host target if it's available.
            val default = TargetVariant.host
            if (candidateValues.contains(default)) {
                closestMatch(default)
            }
        }
    }
}

/**
 * Register [TargetVariant] attribute with [AttributesSchema].
 */
fun AttributesSchema.registerTargetVariantAttribute() {
    attribute(TargetVariant.TARGET_ATTRIBUTE) {
        disambiguationRules.add(TargetDisambiguationRule::class.java)
    }
}