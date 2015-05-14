package org.jetbrains.idl2k

import org.antlr.v4.runtime.ANTLRFileStream
import java.io.File
import java.io.StringReader
import java.util.*

fun main(args: Array<String>) {
    val outDir = File("../../../js/js.libraries/src/generated")
    val srcDir = File("../../idl")
    if (!srcDir.exists()) {
        System.err?.println("Directory ${srcDir.getAbsolutePath()} doesn't exist")
        System.exit(1)
        return
    }

    val repository = srcDir.walkTopDown().filter { it.isDirectory() || it.extension == "idl" }.asSequence().filter { it.isFile() }.fold(Repository(emptyMap(), emptyMap(), emptyMap(), emptyMap())) { acc, e ->
        val fileRepository = parseIDL(ANTLRFileStream(e.getAbsolutePath(), "UTF-8"))

        Repository(
                interfaces = acc.interfaces.mergeReduce(fileRepository.interfaces, ::merge),
                typeDefs = acc.typeDefs + fileRepository.typeDefs,
                externals = acc.externals merge fileRepository.externals,
                enums = acc.enums + fileRepository.enums
        )
    }

    val definitions = mapDefinitions(repository, repository.interfaces.values()).map {
        if (it.name in relocations) {
            // we need this to get interfaces listed in the relocations in valid package
            // to keep compatibility with DOM Java API
            it.copy(namespace = relocations[it.name]!!)
        } else {
            it
        }
    }
    val unions = generateUnions(definitions, repository.typeDefs.values())
    val allPackages = definitions.map { it.namespace }.distinct().sort()

    outDir.deleteRecursively()
    outDir.mkdirs()

    allPackages.forEach { pkg ->
        File(outDir, pkg + ".kt").bufferedWriter().use { w ->
            w.appendln("/*")
            w.appendln(" * Generated file")
            w.appendln(" * DO NOT EDIT")
            w.appendln(" * ")
            w.appendln(" * See libraries/tools/idl2k for details")
            w.appendln(" */")

            w.appendln()
            w.appendln("package ${pkg}")
            w.appendln()

            allPackages.filter { it != pkg }.forEach { import ->
                w.appendln("import ${import}.*")
            }
            w.appendln()

            w.render(pkg, definitions, unions)
        }
    }
}

private fun <K, V> Map<K, List<V>>.reduceValues(reduce: (V, V) -> V = { a, b -> b }): Map<K, V> = mapValues { it.value.reduce(reduce) }

private fun <K, V> Map<K, V>.mergeReduce(other: Map<K, V>, reduce: (V, V) -> V = { a, b -> b }): Map<K, V> {
    val result = LinkedHashMap<K, V>(this.size() + other.size())
    result.putAll(this)
    other.forEach { e ->
        val existing = result[e.key]

        if (existing == null) {
            result[e.key] = e.value
        }
        else {
            result[e.key] = reduce(e.value, existing)
        }
    }

    return result
}

private fun <K, V> Map<K, List<V>>.merge(other: Map<K, List<V>>): Map<K, List<V>> {
    val result = LinkedHashMap<K, MutableList<V>>(size() + other.size())
    this.forEach {
        result[it.key] = ArrayList(it.value)
    }
    other.forEach {
        val list = result[it.key]
        if (list == null) {
            result[it.key] = ArrayList(it.value)
        }
        else {
            list.addAll(it.value)
        }
    }

    return result
}