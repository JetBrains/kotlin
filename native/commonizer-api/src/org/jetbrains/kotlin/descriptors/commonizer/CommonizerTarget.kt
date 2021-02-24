/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.descriptors.commonizer

import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.Serializable

// N.B. TargetPlatform/SimplePlatform are non exhaustive enough to address both target platforms such as
// JVM, JS and concrete Kotlin/Native targets, e.g. macos_x64, ios_x64, linux_x64.
public sealed class CommonizerTarget : Serializable {
    final override fun toString(): String = identityString
}

public data class LeafCommonizerTarget public constructor(val name: String) : CommonizerTarget() {
    public constructor(konanTarget: KonanTarget) : this(konanTarget.name)

    public val konanTargetOrNull: KonanTarget? = KonanTarget.predefinedTargets[name]

    public val konanTarget: KonanTarget get() = konanTargetOrNull ?: error("Unknown KonanTarget: $name")
}

public data class SharedCommonizerTarget(val targets: Set<CommonizerTarget>) : CommonizerTarget() {
    public constructor(vararg targets: CommonizerTarget) : this(targets.toSet())
    public constructor(vararg targets: KonanTarget) : this(targets.toSet())
    public constructor(targets: Iterable<KonanTarget>) : this(targets.map(::LeafCommonizerTarget).toSet())

    init {
        require(targets.isNotEmpty())
    }
}

public fun CommonizerTarget(konanTargets: Iterable<KonanTarget>): CommonizerTarget {
    val konanTargetsSet = konanTargets.toSet()
    require(konanTargetsSet.isNotEmpty()) { "Empty set of of konanTargets" }
    val leafTargets = konanTargetsSet.map(::LeafCommonizerTarget)
    return leafTargets.singleOrNull() ?: SharedCommonizerTarget(leafTargets.toSet())
}

public fun CommonizerTarget(konanTarget: KonanTarget): LeafCommonizerTarget {
    return LeafCommonizerTarget(konanTarget)
}

public fun CommonizerTarget(konanTarget: KonanTarget, vararg konanTargets: KonanTarget): SharedCommonizerTarget {
    val targets = ArrayList<KonanTarget>(konanTargets.size + 1).apply {
        add(konanTarget)
        addAll(konanTargets)
    }
    return SharedCommonizerTarget(targets.map(::LeafCommonizerTarget).toSet())
}

public val CommonizerTarget.identityString: String
    get() = when (this) {
        is LeafCommonizerTarget -> name
        is SharedCommonizerTarget -> identityString
    }

private val SharedCommonizerTarget.identityString: String
    get() {
        val segments = targets.map(CommonizerTarget::identityString).sorted()
        return segments.joinToString(
            separator = ", ", prefix = "(", postfix = ")"
        )
    }

public val CommonizerTarget.prettyName: String
    get() = when (this) {
        is LeafCommonizerTarget -> "[$name]"
        is SharedCommonizerTarget -> prettyName(null)
    }

public fun SharedCommonizerTarget.prettyName(highlightedChild: CommonizerTarget?): String {
    return targets
        .sortedWith(compareBy<CommonizerTarget> { it.level }.thenBy { it.identityString }).joinToString(", ", "[", "]") { child ->
            when (child) {
                is LeafCommonizerTarget -> child.name
                is SharedCommonizerTarget -> child.prettyName(highlightedChild)
            } + if (child == highlightedChild) "(*)" else ""
        }
}

public val CommonizerTarget.konanTargets: Set<KonanTarget>
    get() {
        return when (this) {
            is LeafCommonizerTarget -> setOf(konanTarget)
            is SharedCommonizerTarget -> targets.flatMap { it.konanTargets }.toSet()
        }
    }

public val CommonizerTarget.level: Int
    get() {
        return when (this) {
            is LeafCommonizerTarget -> return 0
            is SharedCommonizerTarget -> targets.maxOf { it.level } + 1
        }
    }
