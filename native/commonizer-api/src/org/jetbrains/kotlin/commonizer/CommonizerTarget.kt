/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.commonizer

import org.jetbrains.kotlin.commonizer.util.transitiveClosure
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.Serializable

// N.B. TargetPlatform/SimplePlatform are non exhaustive enough to address both target platforms such as
// JVM, JS and concrete Kotlin/Native targets, e.g. macos_x64, ios_x64, linux_x64.
public sealed class CommonizerTarget : Serializable {
    final override fun toString(): String = prettyName
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

    public companion object {
        public fun ifNotEmpty(targets: Set<CommonizerTarget>): SharedCommonizerTarget? {
            return if (targets.isNotEmpty()) SharedCommonizerTarget(targets) else null
        }
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

public fun CommonizerTarget(commonizerTarget: CommonizerTarget, vararg commonizerTargets: CommonizerTarget): SharedCommonizerTarget {
    val targets = mutableListOf<CommonizerTarget>().apply {
        add(commonizerTarget)
        addAll(commonizerTargets)
    }
    return SharedCommonizerTarget(targets.toSet())
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
            is SharedCommonizerTarget -> if (targets.isNotEmpty()) targets.maxOf { it.level } + 1 else 0
        }
    }

public fun CommonizerTarget.withAllAncestors(): Set<CommonizerTarget> {
    return setOf(this) + transitiveClosure(this) {
        when (this) {
            is SharedCommonizerTarget -> targets
            is LeafCommonizerTarget -> emptyList()
        }
    }
}

public fun CommonizerTarget.allLeaves(): Set<LeafCommonizerTarget> {
    return withAllAncestors().filterIsInstance<LeafCommonizerTarget>().toSet()
}

public infix fun CommonizerTarget.isAncestorOf(other: CommonizerTarget): Boolean {
    if (this is SharedCommonizerTarget) {
        return targets.any { it == other } || targets.any { it.isAncestorOf(other) }
    }
    return false
}

public infix fun CommonizerTarget.isEqualOrAncestorOf(other: CommonizerTarget): Boolean {
    return this == other || this.isAncestorOf(other)
}

public infix fun CommonizerTarget.isDescendentOf(other: CommonizerTarget): Boolean {
    return other.isAncestorOf(this)
}
