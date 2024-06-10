/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation.api.klib

import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.Comparator

private class PeekingLineIterator(private val lines: Iterator<String>) : Iterator<String> {
    private var nextLine: String? = null

    public fun peek(): String? {
        if (nextLine != null) {
            return nextLine
        }
        if (!lines.hasNext()) {
            return null
        }
        nextLine = lines.next()
        return nextLine
    }

    override fun hasNext(): Boolean {
        return nextLine != null || lines.hasNext()
    }

    override fun next(): String {
        if (nextLine != null) {
            val res = nextLine!!
            nextLine = null
            return res
        }
        return lines.next()
    }
}

private const val MERGED_DUMP_FILE_HEADER = "// Klib ABI Dump"
private const val REGULAR_DUMP_FILE_HEADER = "// Rendering settings:"
private const val COMMENT_PREFIX = "//"
private const val TARGETS_LIST_PREFIX = "// Targets: ["
private const val TARGETS_LIST_SUFFIX = "]"
private const val TARGETS_DELIMITER = ", "
private const val CLASS_DECLARATION_TERMINATOR = "}"
private const val INDENT_WIDTH = 4
private const val ALIAS_PREFIX = "// Alias: "
private const val PLATFORM_PREFIX = "// Platform: "
private const val NATIVE_TARGETS_PREFIX = "// Native targets: "
private const val LIBRARY_NAME_PREFIX = "// Library unique name:"

private fun String.depth(): Int {
    val indentation = this.takeWhile { it == ' ' }.count()
    require(indentation % INDENT_WIDTH == 0) {
        "Unexpected indentation, should be a multiple of $INDENT_WIDTH: $this"
    }
    return indentation / INDENT_WIDTH
}

private fun parseBcvTargetsLine(line: String): Set<KlibTarget> {
    val trimmedLine = line.trimStart(' ')
    check(trimmedLine.startsWith(TARGETS_LIST_PREFIX) && trimmedLine.endsWith(TARGETS_LIST_SUFFIX)) {
        "Not a targets list line: \"$line\""
    }
    return trimmedLine.substring(TARGETS_LIST_PREFIX.length, trimmedLine.length - 1)
        .split(TARGETS_DELIMITER)
        .map { KlibTarget.parse(it) }
        .toSet()
}

private class KlibAbiDumpHeader(
    val content: List<String>,
    val underlyingTargets: Set<KlibTarget>
) {
    constructor(content: List<String>, underlyingTarget: KlibTarget) : this(content, setOf(underlyingTarget))
}

/**
 * A class representing a textual KLib ABI dump, either a regular one, or a merged.
 */
internal class KlibAbiDumpMerger {
    private val _targets: MutableSet<KlibTarget> = mutableSetOf()
    private val headerContent: MutableList<String> = mutableListOf()
    private val topLevelDeclaration: DeclarationContainer = DeclarationContainer("")

    /**
     * All targets for which this dump contains declarations.
     */
    internal val targets: Set<KlibTarget> = _targets

    internal fun merge(file: File, configurableTargetName: String? = null) {
        require(file.exists()) { "File does not exist: $file" }
        // TODO: replace with file.toPath().useLines once language version get upgraded
        Files.lines(file.toPath()).use {
            merge(it.iterator(), configurableTargetName)
        }
    }

    internal fun merge(lines: Iterator<String>, configurableTargetName: String? = null) {
        merge(PeekingLineIterator(lines), configurableTargetName)
    }

    private fun merge(lines: PeekingLineIterator, configurableTargetName: String?) {
        require(lines.peek() != null) { "File is empty" }
        val isMergedFile = lines.determineFileType()

        val aliases = mutableMapOf<String, Set<KlibTarget>>()
        val bcvTargets = mutableSetOf<KlibTarget>()
        if (isMergedFile) {
            lines.next() // skip the heading line
            bcvTargets.addAll(lines.parseTargets(configurableTargetName))
            check(bcvTargets.size == 1 || configurableTargetName == null) {
                "Can't use an explicit target name with a multi-target dump. " +
                        "targetName: $configurableTargetName, dump targets: $bcvTargets"
            }
            aliases.putAll(lines.parseAliases())
        }
        val header = lines.parseFileHeader(isMergedFile, configurableTargetName)
        bcvTargets.addAll(header.underlyingTargets)
        bcvTargets.intersect(targets).also {
            check(it.isEmpty()) { "This dump and a file to merge share some targets: $it" }
        }

        if (this._targets.isEmpty()) {
            headerContent.addAll(header.content)
        } else if (headerContent != header.content) {
            throw IllegalStateException(
                "File header doesn't match the header of other files\n"
                        + headerContent.toString() + "\n\n\n" + header.content.toString()
            )
        }
        this._targets.addAll(bcvTargets)
        topLevelDeclaration.targets.addAll(bcvTargets)

        // All declarations belonging to the same scope have equal indentation.
        // Nested declarations have higher indentation.
        // By tracking the indentation, we can decide if the line should be added into the current container,
        // to its parent container (i.e., the line represents sibling declaration) or the current declaration ended,
        // and we must pop one or several declarations out of the parsing stack.
        var currentContainer = topLevelDeclaration
        var depth = -1
        val targetsStack = mutableListOf<Set<KlibTarget>>().apply { add(bcvTargets) }

        while (lines.hasNext()) {
            val line = lines.peek()!!
            if (line.isEmpty()) {
                lines.next(); continue
            }
            // TODO: wrap the line and cache the depth inside that wrapper?
            val lineDepth = line.depth()
            when {
                // The depth is the same as before; we encountered a sibling
                depth == lineDepth -> {
                    // pop it off to swap previous value from the same depth,
                    // parseDeclaration will update it
                    targetsStack.removeLast()
                    currentContainer =
                        lines.parseDeclaration(lineDepth, currentContainer.parent!!, targetsStack, aliases)
                }
                // The depth is increasing; that means we encountered child declaration
                depth < lineDepth -> {
                    check(lineDepth - depth == 1) {
                        "The line has too big indentation relative to a previous line\nline: $line\n" +
                                "previous: ${currentContainer.text}"
                    }
                    currentContainer =
                        lines.parseDeclaration(lineDepth, currentContainer, targetsStack, aliases)
                    depth = lineDepth
                }
                // Otherwise, we're finishing all the declaration with greater depth compared to the depth of
                // the next line.
                // We won't process a line if it contains a new declaration here, just update the depth and current
                // declaration reference to process the new declaration on the next iteration.
                else -> {
                    while (currentContainer.text.depth() > lineDepth) {
                        currentContainer = currentContainer.parent!!
                        targetsStack.removeLast()
                    }
                    // If the line is '}' - add it as a terminator to the corresponding declaration, it'll simplify
                    // dumping the merged file back to text format.
                    if (line.trim() == CLASS_DECLARATION_TERMINATOR) {
                        currentContainer.delimiter = line
                        // We processed the terminator char, so let's skip this line.
                        lines.next()
                    }
                    // For the top level declaration depth is -1
                    depth = if (currentContainer.parent == null) -1 else currentContainer.text.depth()
                }
            }
        }
    }

    private fun PeekingLineIterator.parseTargets(configurableTargetName: String?): Set<KlibTarget> {
        val line = peek()
        require(line != null) {
            "List of targets expected, but there are no more lines left."
        }
        require(line.startsWith(TARGETS_LIST_PREFIX)) {
            "The line should starts with $TARGETS_LIST_PREFIX, but was: $line"
        }
        next()
        val targets = parseBcvTargetsLine(line)
        require(configurableTargetName == null || targets.size == 1) {
            "Can't use configurableTargetName ($configurableTargetName) for a multi-target dump: $targets"
        }
        if (configurableTargetName != null) {
            return setOf(KlibTarget(targets.first().targetName, configurableTargetName))
        }
        return targets
    }

    private fun PeekingLineIterator.parseAliases(): Map<String, Set<KlibTarget>> {
        val aliases = mutableMapOf<String, Set<KlibTarget>>()
        while (peek()?.startsWith(ALIAS_PREFIX) == true) {
            val line = next()
            val trimmedLine = line.substring(ALIAS_PREFIX.length)
            val separatorIdx = trimmedLine.indexOf(" => [")
            if (separatorIdx == -1 || !trimmedLine.endsWith(']')) {
                throw IllegalStateException("Invalid alias line: $line")
            }
            val name = trimmedLine.substring(0, separatorIdx)
            val targets = trimmedLine.substring(
                separatorIdx + " => [".length,
                trimmedLine.length - 1
            )
                .split(",")
                .map { KlibTarget.parse(it.trim()) }
                .toSet()
            aliases[name] = targets
        }
        return aliases
    }

    private fun PeekingLineIterator.parseFileHeader(
        isMergedFile: Boolean,
        configurableTargetName: String?
    ): KlibAbiDumpHeader {
        val header = mutableListOf<String>()
        var targets: String? = null
        var platform: String? = null

        // read the common head first
        while (hasNext()) {
            val next = peek()!!
            if (next.isNotBlank() && !next.startsWith(COMMENT_PREFIX)) {
                throw IllegalStateException("Library header has invalid format at line \"$next\"")
            }
            header.add(next)
            next()
            if (next.startsWith(LIBRARY_NAME_PREFIX)) {
                break
            }
        }
        // then try to parse a manifest
        while (hasNext()) {
            val next = peek()!!
            if (!next.startsWith(COMMENT_PREFIX) || next.startsWith(TARGETS_LIST_PREFIX)) break
            next()
            // There's no manifest in merged files
            check(!isMergedFile) { "Unexpected header line: $next" }
            when {
                next.startsWith(PLATFORM_PREFIX) -> {
                    platform = next.split(": ")[1].trim()
                }

                next.startsWith(NATIVE_TARGETS_PREFIX) -> {
                    targets = next.split(": ")[1].trim()
                }
            }
        }
        if (isMergedFile) {
            return KlibAbiDumpHeader(header, emptySet())
        }

        // transform a combination of platform name and targets list to a set of KlibTargets
        return KlibAbiDumpHeader(header, extractTargets(platform, targets, configurableTargetName))
    }

    private fun extractTargets(
        platformString: String?,
        targetsString: String?,
        configurableTargetName: String?
    ): Set<KlibTarget> {
        check(platformString != null) {
            "The dump does not contain platform name. Please make sure that the manifest was included in the dump"
        }

        if (platformString == "WASM") {
            // Currently, there's no way to distinguish Wasm targets without explicitly specifying a target name
            check(configurableTargetName != null) { "targetName has to be specified for a Wasm target" }
            return setOf(KlibTarget(configurableTargetName))
        }
        if (platformString != "NATIVE") {
            val platformStringLc = platformString.toLowerCase(Locale.ROOT)
            return if (configurableTargetName == null) {
                setOf(KlibTarget(platformStringLc))
            } else {
                setOf(KlibTarget(platformStringLc, configurableTargetName))
            }
        }

        check(targetsString != null) { "Dump for a native platform missing targets list." }

        val targetsList = targetsString.split(TARGETS_DELIMITER).map {
            konanTargetNameMapping[it.trim()] ?: throw IllegalStateException("Unknown native target: $it")
        }
        require(targetsList.size == 1 || configurableTargetName == null) {
            "Can't use configurableTargetName ($configurableTargetName) for a multi-target dump: $targetsList"
        }
        if (targetsList.size == 1 && configurableTargetName != null) {
            return setOf(KlibTarget(targetsList.first(), configurableTargetName))
        }
        return targetsList.asSequence().map { KlibTarget(it) }.toSet()
    }

    private fun PeekingLineIterator.determineFileType(): Boolean {
        val headerLine = peek() ?: throw IllegalStateException("File is empty")
        if (headerLine.trimEnd() == MERGED_DUMP_FILE_HEADER) {
            return true
        }
        if (headerLine.trimEnd() == REGULAR_DUMP_FILE_HEADER) {
            return false
        }
        val headerStart = if (headerLine.length > 32) {
            headerLine.substring(0, 32) + "..."
        } else {
            headerLine
        }
        throw IllegalStateException(
            "Expected a file staring with \"$REGULAR_DUMP_FILE_HEADER\" " +
                    "or \"$MERGED_DUMP_FILE_HEADER\", but the file stats with \"$headerStart\""
        )
    }

    private fun PeekingLineIterator.parseDeclaration(
        depth: Int,
        parent: DeclarationContainer,
        parentTargetsStack: MutableList<Set<KlibTarget>>,
        aliases: Map<String, Set<KlibTarget>>
    ): DeclarationContainer {
        val line = peek()!!
        return if (line.startsWith(" ".repeat(depth * INDENT_WIDTH) + TARGETS_LIST_PREFIX)) {
            next() // skip prefix
            // Target list means that the declaration following it has a narrower set of targets than its parent,
            // so we must use it.
            val targets = parseBcvTargetsLine(line)
            val expandedTargets = targets.flatMap {
                aliases[it.configurableName] ?: listOf(it)
            }.toSet()
            parentTargetsStack.add(expandedTargets)
            parent.createOrUpdateChildren(next(), expandedTargets)
        } else {
            // Inherit all targets from a parent
            parentTargetsStack.add(parentTargetsStack.last())
            parent.createOrUpdateChildren(next(), parentTargetsStack.last())
        }
    }

    fun dump(appendable: Appendable) {
        if (targets.isEmpty()) {
            check(topLevelDeclaration.children.isEmpty()) {
                "Dump containing some declaration should have at least a single target"
            }
            return
        }
        val formatter = createFormatter()
        appendable.append(MERGED_DUMP_FILE_HEADER).append('\n')
        appendable.append(formatter.formatHeader(targets)).append('\n')
        headerContent.forEach {
            appendable.append(it).append('\n')
        }
        topLevelDeclaration.children.values.sortedWith(DeclarationsComparator).forEach {
            it.dump(appendable, _targets, formatter)
        }
    }

    private fun createFormatter(): KlibsTargetsFormatter {
        for (target in targets) {
            val node = TargetHierarchy.hierarchyIndex[target.targetName]
            if (node != null && node.allLeafs.size == 1 && node.allLeafs.first() != node.node.name) {
                throw IllegalStateException(
                    "Can't use target aliases as one of the this dump's targets" +
                            " has the same name as a group in the default targets hierarchy: $target"
                )
            }
        }
        return KlibsTargetsFormatter(this)
    }

    /**
     * Remove the [target] from this dump.
     * If some declaration was declared only for [target], it will be removed from the dump.
     */
    fun remove(target: KlibTarget) {
        if (!_targets.remove(target)) {
            return
        }
        topLevelDeclaration.remove(target)
    }

    /**
     * Leave only declarations specific to a [target].
     * A declaration is considered target-specific if:
     * 1) it defined for some [targets] subset including [target], but not for all [targets];
     * 2) it defined for all [targets], but contains target-specific child declaration.
     */
    fun retainTargetSpecificAbi(target: KlibTarget) {
        if (!_targets.contains(target)) {
            _targets.clear()
            topLevelDeclaration.children.clear()
            topLevelDeclaration.targets.clear()
            return
        }

        topLevelDeclaration.retainSpecific(target, _targets)
        _targets.retainAll(setOf(target))
    }

    /**
     * Remove all declarations that are not defined for all [KlibAbiDumpMerger.targets].
     */
    fun retainCommonAbi() {
        topLevelDeclaration.retainCommon(_targets)
        if (topLevelDeclaration.children.isEmpty()) {
            _targets.clear()
        }
    }

    /**
     * Merge the [other] dump containing declarations for a single target into this dump.
     * The dump [other] should contain exactly one target and this dump should not contain that target.
     */
    fun mergeTargetSpecific(other: KlibAbiDumpMerger) {
        require(other._targets.size == 1) {
            "The dump to merge in should have a single target, but its targets are: ${other.targets}"
        }
        require(other._targets.first() !in _targets) {
            "Targets of this dump and the dump to merge into it should not intersect. " +
                    "Common target: ${other.targets.first()}}"
        }

        _targets.addAll(other._targets)
        topLevelDeclaration.mergeTargetSpecific(other.topLevelDeclaration)
    }

    /**
     * Merges other [KlibAbiDumpMerger] into this one.
     */
    fun merge(other: KlibAbiDumpMerger) {
        if (other.targets.isEmpty()) return

        targets.intersect(other.targets).also {
            require(it.isEmpty()) {
                "Targets of this dump and the dump to merge into it should not intersect. Common targets: $it"
            }
        }
        if (headerContent != other.headerContent) {
            // the dump was empty
            if (headerContent.isEmpty() && targets.isEmpty()) {
                headerContent.addAll(other.headerContent)
            } else {
                throw IllegalArgumentException("Dumps headers does not match")
            }
        }

        _targets.addAll(other._targets)
        topLevelDeclaration.merge(other.topLevelDeclaration)
    }

    /**
     * For each declaration change targets to a specified [targets] set.
     */
    fun overrideTargets(targets: Set<KlibTarget>) {
        _targets.clear()
        _targets.addAll(targets)

        topLevelDeclaration.overrideTargets(targets)
    }

    internal fun visit(action: (DeclarationContainer) -> Unit) {
        topLevelDeclaration.children.values.forEach(action)
    }
}

/**
 * A class representing a single declaration from a KLib API dump along with all its children
 * declarations.
 */
internal class DeclarationContainer(val text: String, val parent: DeclarationContainer? = null) {
    val type: DeclarationType? = if (text.isNotBlank()) DeclarationType.parseFromDeclaration(text) else null
    val targets: MutableSet<KlibTarget> = mutableSetOf()
    val children: MutableMap<String, DeclarationContainer> = mutableMapOf()
    var delimiter: String? = null

    fun createOrUpdateChildren(text: String, targets: Set<KlibTarget>): DeclarationContainer {
        val child = children.computeIfAbsent(text) {
            val newChild = DeclarationContainer(it, this)
            newChild
        }
        child.targets.addAll(targets)
        return child
    }

    fun dump(appendable: Appendable, allTargets: Set<KlibTarget>, formatter: KlibsTargetsFormatter) {
        if (targets != allTargets/* && !dumpFormat.singleTargetDump*/) {
            // Use the same indentation for target list as for the declaration itself
            appendable.append(" ".repeat(text.depth() * INDENT_WIDTH))
                .append(formatter.formatDeclarationTargets(targets))
                .append('\n')
        }
        appendable.append(text).append('\n')
        children.values.sortedWith(DeclarationsComparator).forEach {
            it.dump(appendable, this.targets, formatter)
        }
        if (delimiter != null) {
            appendable.append(delimiter).append('\n')
        }
    }

    fun remove(target: KlibTarget) {
        if (parent != null && !targets.contains(target)) {
            return
        }
        targets.remove(target)
        mutateChildrenAndRemoveTargetless { it.remove(target) }
    }

    fun retainSpecific(target: KlibTarget, allTargets: Set<KlibTarget>) {
        if (parent != null && !targets.contains(target)) {
            children.clear()
            targets.clear()
            return
        }

        mutateChildrenAndRemoveTargetless { it.retainSpecific(target, allTargets) }

        if (targets == allTargets) {
            if (children.isEmpty()) {
                targets.clear()
            } else {
                targets.retainAll(setOf(target))
            }
        } else {
            targets.retainAll(setOf(target))
        }
    }

    fun retainCommon(commonTargets: Set<KlibTarget>) {
        if (parent != null && targets != commonTargets) {
            children.clear()
            targets.clear()
            return
        }
        mutateChildrenAndRemoveTargetless { it.retainCommon(commonTargets) }
    }

    fun mergeTargetSpecific(other: DeclarationContainer) {
        targets.addAll(other.targets)
        other.children.forEach { otherChild ->
            when (val child = children[otherChild.key]) {
                null -> {
                    children[otherChild.key] = otherChild.value.deepCopy(this)
                }
                else -> child.mergeTargetSpecific(otherChild.value)
            }
        }
        children.forEach {
            if (other.targets.first() !in it.value.targets) {
                it.value.addTargetRecursively(other.targets.first())
            }
        }
    }

    fun merge(other: DeclarationContainer) {
        targets.addAll(other.targets)
        val parent = this
        other.children.forEach { (line, decl) ->
            children.compute(line) { _, thisDecl ->
                if (thisDecl == null) {
                    decl.deepCopy(parent)
                } else {
                    thisDecl.apply { merge(decl) }
                }
            }
        }
    }

    fun deepCopy(parent: DeclarationContainer): DeclarationContainer {
        val copy = DeclarationContainer(this.text, parent)
        copy.delimiter = delimiter
        copy.targets.addAll(targets)
        children.forEach { key, value ->
            copy.children[key] = value.deepCopy(copy)
        }
        return copy
    }

    private fun addTargetRecursively(first: KlibTarget) {
        targets.add(first)
        children.forEach { it.value.addTargetRecursively(first) }
    }

    fun overrideTargets(targets: Set<KlibTarget>) {
        this.targets.clear()
        this.targets.addAll(targets)
        children.forEach { it.value.overrideTargets(targets) }
    }

    private inline fun mutateChildrenAndRemoveTargetless(blockAction: (DeclarationContainer) -> Unit) {
        val iterator = children.iterator()
        while (iterator.hasNext()) {
            val (_, child) = iterator.next()
            blockAction(child)
            if (child.targets.isEmpty()) {
                iterator.remove()
            }
        }
    }

    internal fun visit(action: (DeclarationContainer) -> Unit) {
        children.forEach {
            action(it.value)
        }
    }
}

// TODO: optimize
private object DeclarationsComparator : Comparator<DeclarationContainer> {
    private fun Set<KlibTarget>.serializeAndSort(): MutableList<String> {
        return this.mapTo(mutableListOf()) { it.toString() }.apply { sort() }
    }

    private fun compareStructurally(lhs: DeclarationContainer, rhs: DeclarationContainer): Int {
        require(lhs.parent === rhs.parent)

        val isTopLevel = lhs.parent?.parent == null

        fun extractOrderMod(c: DeclarationContainer): Int {
            val ord = if (isTopLevel) {
                c.type?.topLevelOrder
            } else {
                c.type?.nestedOrder
            }
            return ord ?: DeclarationType.last
        }

        val lhsOrderMod = extractOrderMod(lhs)
        val rhsOrderMod = extractOrderMod(rhs)

        return if (lhsOrderMod == rhsOrderMod) {
            lhs.text.compareTo(rhs.text)
        } else {
            lhsOrderMod.compareTo(rhsOrderMod)
        }
    }

    override fun compare(c0: DeclarationContainer, c1: DeclarationContainer): Int {
        return if (c0.targets == c1.targets) {
            compareStructurally(c0, c1)
        } else {
            if (c0.targets.size == c1.targets.size) {
                val c0targets = c0.targets.serializeAndSort().iterator()
                val c1targets = c1.targets.serializeAndSort().iterator()
                var result = 0
                while (c1targets.hasNext() && c0targets.hasNext() && result == 0) {
                    result = c0targets.next().compareTo(c1targets.next())
                }
                result
            } else {
                // the longer the target list, the earlier the declaration would appear
                c1.targets.size.compareTo(c0.targets.size)
            }
        }
    }
}

internal class KlibsTargetsFormatter(klibDump: KlibAbiDumpMerger) {
    private data class Alias(val name: String, val targets: Set<KlibTarget>)

    private val aliases: List<Alias>

    init {
        // place more specific groups (with a higher depth value) closer to the beginning of the list
        val nodesDescendingComparator =
            compareByDescending<Map.Entry<String, TargetHierarchy.NodeClosure>> { it.value.depth }
                .thenByDescending { it.key }
        val allTargets = klibDump.targets
        val aliasesBuilder = mutableListOf<Alias>()
        TargetHierarchy.hierarchyIndex.entries
            .sortedWith(nodesDescendingComparator)
            .forEach {
                // intersect with all targets to use only enabled targets in aliases
                // intersection is based on underlying target name as a set of such names is fixed
                val leafs = it.value.allLeafs
                val availableTargets = allTargets.asSequence().filter { leafs.contains(it.targetName) }.toSet()
                if (availableTargets.isNotEmpty()) {
                    aliasesBuilder.add(Alias(it.key, availableTargets))
                }
            }

        // filter out all groups consisting of less than one member
        aliasesBuilder.removeIf { it.targets.size < 2 }
        aliasesBuilder.removeIf { it.targets == allTargets }
        filterOutDumplicateGroups(aliasesBuilder)
        filterOutUnusedGroups(klibDump, aliasesBuilder)

        // reverse the order to place a common group first
        aliases = aliasesBuilder.reversed()
    }

    private fun filterOutDumplicateGroups(allGroups: MutableList<Alias>) {
        // Remove all duplicating groups. At this point, aliases are sorted so
        // that more specific groups are before more general groups, so we'll remove
        // more common groups here.
        for (idx in allGroups.size - 1 downTo 1) {
            if (allGroups[idx].targets == allGroups[idx - 1].targets) {
                // TODO: can we avoid shifting the whole trailing part of the list?
                allGroups.removeAt(idx)
            }
        }
    }

    // TODO: optimize the algorithm
    private fun filterOutUnusedGroups(klibDump: KlibAbiDumpMerger, allGroups: MutableList<Alias>) {
        // Collect all target sets that are actually in use
        val targetSetsInUse = mutableSetOf<Set<KlibTarget>>()
        val allTargets = klibDump.targets
        fun visitor(decl: DeclarationContainer) {
            if (decl.targets != allTargets) {
                targetSetsInUse.add(decl.targets)
            }
            decl.visit(::visitor)
        }
        klibDump.visit(::visitor)
        // Scan groups from general to specific and check it there are some
        // declarations having a target set such that it includes a group.
        // If there are no such declarations - a group has to be removed.
        for (idx in allGroups.size - 1 downTo 0) {
            val alias = allGroups[idx]
            val updatedTargetSets = mutableSetOf<Set<KlibTarget>>()
            // scan actually used target sets
            val targetSetIterator = targetSetsInUse.iterator()
            while (targetSetIterator.hasNext()) {
                val s = targetSetIterator.next()
                // If a target set includes this group, take that set, remove all targets
                // corresponding to the current group from it and then, later, add the set back.
                if (s.containsAll(alias.targets)) {
                    targetSetIterator.remove()
                    updatedTargetSets.add(s.subtract(alias.targets))
                }
            }
            // If updatedTargetSets is empty, there are no target sets including the current group,
            // so we have to remove the group.
            if (updatedTargetSets.isEmpty()) {
                allGroups.removeAt(idx)
            } else {
                targetSetsInUse.addAll(updatedTargetSets)
            }
        }
    }

    fun formatHeader(targets: Set<KlibTarget>): String {
        return buildString {
            append(
                targets.asSequence().map { it.toString() }.sorted().joinToString(
                    prefix = TARGETS_LIST_PREFIX,
                    postfix = TARGETS_LIST_SUFFIX,
                    separator = TARGETS_DELIMITER
                )
            )
            aliases.forEach {
                append("\n$ALIAS_PREFIX${it.name} => [")
                append(it.targets.map { it.toString() }.sorted().joinToString(TARGETS_DELIMITER))
                append(TARGETS_LIST_SUFFIX)
            }
        }
    }

    fun formatDeclarationTargets(targets: Set<KlibTarget>): String {
        val mutableTargets = targets.toMutableSet()
        val resultingTargets = mutableListOf<String>()
        for (alias in aliases) {
            if (mutableTargets.containsAll(alias.targets)) {
                mutableTargets.removeAll(alias.targets)
                resultingTargets.add(alias.name)
            }
        }
        resultingTargets.addAll(mutableTargets.map { it.toString() })
        return resultingTargets.sorted().joinToString(
            prefix = TARGETS_LIST_PREFIX,
            postfix = TARGETS_LIST_SUFFIX,
            separator = TARGETS_DELIMITER
        )
    }
}

internal enum class DeclarationType(val topLevelOrder: Int, val nestedOrder: Int) {
    Class(3, 9),
    Object(4, 10),
    Interface(2, 8),
    EnumClass(1, 7),
    AnnotationClass(0, 6),
    Function(8, 5),
    Constructor(100000, 0),
    ConstVal(5, 2),
    Val(6, 3),
    Var(7, 4),
    EnumEntry(100000, 1),
    Unknown(100000, 100000);

    companion object {
        const val last: Int = 100000

        fun parseFromDeclaration(decl: String): DeclarationType {
            val scanner = Scanner(decl.trim())
            while (scanner.hasNext()) {
                when (val part = scanner.next()) {
                    "final", "open", "abstract", "sealed", "suspend", "inline", "inner", "value" -> {
                        /* do nothing; we only need declaration type info (i.e., class, function, property, etc.) */
                        continue
                    }

                    "enum" -> {
                        when (val next = scanner.next()) {
                            "entry" -> return EnumEntry
                            "class" -> return EnumClass
                            else -> throw IllegalStateException("Unexpected token '$next' after '$part'")
                        }
                    }

                    "constructor" -> return Constructor
                    "val" -> return Val
                    "const" -> {
                        val type = ConstVal
                        check(scanner.next() == "val") { "expected 'val'" }
                        return type
                    }

                    "var" -> return Var
                    "interface" -> return Interface
                    "annotation" -> {
                        val type = AnnotationClass
                        check(scanner.next() == "class") { "expected 'class'" }
                        return type
                    }

                    "class" -> return Class
                    "object" -> return Object
                    "fun" -> {
                        val next = scanner.next()
                        when (next) {
                            "class" -> return Class
                            "interface" -> return Interface
                            "object" -> return Object

                            "enum" -> {
                                val type = EnumClass
                                check(scanner.next() == "class") { "expected 'class'" }
                                return type
                            }

                            "annotation" -> {
                                val type = AnnotationClass
                                check(scanner.next() == "class") { "expected 'class'" }
                                return type
                            }

                            else -> return Function
                        }
                    }
                }
            }
            return Unknown
        }
    }
}
