/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

import java.io.EOFException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption

private val archiveMagic = "!<arch>\n".toByteArray()
private const val MEMBER_HEADER_SIZE = 60
private const val BSD_LONG_NAME_PREFIX = "#1/"
private const val SYMBOL_INDEX_NAME_PREFIX = "__.SYMDEF"

// A multiple of 16, so that the chunks of the symbol table consist of whole entries.
private const val CHUNK_SIZE = 64 * 1024

/**
 * Zeroes the padding at the end of the symbol name table in the symbol index (the `__.SYMDEF*` member)
 * of the BSD archive [archive] created by Apple's libtool.
 *
 * libtool rounds the size of the name table up to a multiple of 8 bytes. Since Xcode 27, it leaves garbage
 * in the padding, which makes the output not reproducible (KT-89897).
 */
internal fun zeroArchiveSymbolTablePadding(archive: Path) {
    FileChannel.open(archive, StandardOpenOption.READ, StandardOpenOption.WRITE).use { channel ->
        val header = channel.readFully(0, archiveMagic.size + MEMBER_HEADER_SIZE) ?: return
        if (!archiveMagic.indices.all { header[it] == archiveMagic[it] }) return

        val headerFields = String(header,archiveMagic.size, MEMBER_HEADER_SIZE, Charsets.US_ASCII)
        val nameField = headerFields.substring(0, 16).trimEnd()
        val memberSize = headerFields.substring(48, 58).trim().toLongOrNull()?.takeIf { it >= 0 } ?: return
        var dataStart = (archiveMagic.size + MEMBER_HEADER_SIZE).toLong()

        // A long name follows the header, and the member size includes it.
        val name = if (nameField.startsWith(BSD_LONG_NAME_PREFIX)) {
            val nameLength = nameField.removePrefix(BSD_LONG_NAME_PREFIX).toLongOrNull() ?: return
            if (nameLength !in 0..memberSize) return
            // Only the first 32 bytes of the name is checked.
            val nameBytes = channel.readFully(dataStart, minOf(nameLength, 32).toInt()) ?: return
            dataStart += nameLength
            String(nameBytes, Charsets.US_ASCII).trimEnd('\u0000')
        } else nameField
        if (!name.startsWith(SYMBOL_INDEX_NAME_PREFIX)) return
        val wordSize = if (name.startsWith("${SYMBOL_INDEX_NAME_PREFIX}_64")) 8 else 4
        val dataEnd = archiveMagic.size + MEMBER_HEADER_SIZE + memberSize
        if (dataEnd > channel.size()) return

        // The member data:
        val symbolTableSize = channel.readWord(dataStart, wordSize) ?: return
        val symbolTableStart = dataStart + wordSize
        if (symbolTableSize > dataEnd - symbolTableStart - wordSize) return
        val symbolTableEnd = symbolTableStart + symbolTableSize
        val nameTableSize = channel.readWord(symbolTableEnd, wordSize) ?: return
        val nameTableStart = symbolTableEnd + wordSize
        if (nameTableSize > dataEnd - nameTableStart) return
        val nameTableEnd = nameTableStart + nameTableSize

        // The name with the largest offset ends last: any name that starts before it ends either before it or at the same '\0'.
        val entrySize = 2 * wordSize
        val entriesEnd = symbolTableStart + symbolTableSize / entrySize * entrySize
        var lastNameOffset = 0L // inside the name table
        for (chunk in channel.chunks(symbolTableStart, entriesEnd)) {
            for (entry in 0 until chunk.bytes.size / entrySize) {
                val nameOffset = chunk.bytes.getWord(entry * entrySize, wordSize) ?: return
                lastNameOffset = maxOf(lastNameOffset, nameOffset)
            }
        }

        // The padding is everything after the end of the last name.
        if (lastNameOffset >= nameTableSize) return
        val paddingStart = channel.positionAfterFirstZero(nameTableStart + lastNameOffset, nameTableEnd) ?: return
        // libtool pads to a multiple of 8 bytes, so larger padding isn't expected and isn't supported.
        val paddingSize = (nameTableEnd - paddingStart).takeIf { it <= Int.MAX_VALUE } ?: return
        channel.writeFully(ByteArray(paddingSize.toInt()), paddingStart)
    }
}

private class Chunk(val position: Long, val bytes: ByteArray)

// The bytes in [start, end) in consecutive chunks of up to [chunkSize] bytes.
private fun FileChannel.chunks(start: Long, end: Long, chunkSize: Int = CHUNK_SIZE): Sequence<Chunk> =
    generateSequence(start) { it + chunkSize }
        .takeWhile { it < end }
        .map { position ->
            val bytes = readFully(position, minOf(chunkSize.toLong(), end - position).toInt()) ?: throw EOFException()
            Chunk(position, bytes)
        }

// The position right after the first '\0' in [start, end); `null` if there is none.
private fun FileChannel.positionAfterFirstZero(start: Long, end: Long): Long? =
    chunks(start, end).firstNotNullOfOrNull { chunk ->
        chunk.bytes.indexOf(0).takeIf { it >= 0 }?.let { chunk.position + it + 1 }
    }

private fun FileChannel.readFully(position: Long, size: Int): ByteArray? {
    val bytes = ByteArray(size)
    val buffer = ByteBuffer.wrap(bytes)
    while (buffer.hasRemaining()) {
        if (read(buffer, position + buffer.position()) < 0) return null
    }
    return bytes
}

private fun FileChannel.writeFully(bytes: ByteArray, position: Long) {
    val buffer = ByteBuffer.wrap(bytes)
    while (buffer.hasRemaining()) {
        write(buffer, position + buffer.position())
    }
}

private fun FileChannel.readWord(position: Long, wordSize: Int): Long? = readFully(position, wordSize)?.getWord(0, wordSize)

// Unsigned; `null` if it doesn't fit into a Long - for simplicity
private fun ByteArray.getWord(index: Int, wordSize: Int): Long? {
    require(wordSize == 4 || wordSize == 8)
    // Little-endian.
    return (wordSize - 1 downTo 0).fold(0L) { word, i -> word shl 8 or (this[index + i].toLong() and 0xFF) }.takeIf { it >= 0 }
}
