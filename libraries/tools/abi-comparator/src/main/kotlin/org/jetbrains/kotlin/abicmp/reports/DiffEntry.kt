package org.jetbrains.kotlin.abicmp.reports

class DiffEntry(val value1: String, val value2: String)

class NamedDiffEntry(val name: String, val value1: String, val value2: String)

class ListEntryDiff(val value1: String?, val value2: String?)

class ListDiff(val diff1: List<String>, val diff2: List<String>)

class TextDiffEntry(val lines1: List<String>, val lines2: List<String>)

fun ListEntryDiff.toDiffEntry() =
    DiffEntry(value1 ?: "---", value2 ?: "---")

fun ListEntryDiff.toNamedDiffEntry(name: String) =
    NamedDiffEntry(name, value1 ?: "---", value2 ?: "---")