/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

// The discussion regarding the constant https://github.com/JetBrains/kotlin/pull/5455
internal const val OPTIMAL_SIZE_FOR_REGULAR_LOOP = 16

// The optimal size of a chunk of character codes passed to String.fromCharCode while building
// a string from a character array. The value has been chosen through benchmarking.
//
// Chunking may split a surrogate pair, but it is safe because code unit ordering is preserved
// and so the surrogate pair remains valid in the resulting string.
//
// See discussion at https://github.com/JetBrains/kotlin/pull/5732
internal const val OPTIMAL_CHAR_CODE_CHUNK_SIZE = 4096
