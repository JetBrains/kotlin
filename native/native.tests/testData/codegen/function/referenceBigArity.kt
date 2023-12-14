/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun фу(а: Int, б: Int, в: Int, г: Int, д: Int, е: Int, ё: Int, ж: Int, з: Int, и: Int, й: Int, к: Int,
       л: Int, м: Int, н: Int, о: Int, п: Int, р: Int, с: Int, т: Int, у: Int, ф: Int, х: Int, ц: Int,
       ч: Int, ш: Int, щ: Int, ъ: Int, ы: Int, ь: Int, э: Int, ю: Int, я: Int): Int {
    return а + б + в + г + д + е + ё + ж + з + и + й + к + л + м + н + о +
            п + р + с + т + у + ф + х + ц + ч + ш + щ + ъ + ы + ь + э + ю + я
}

fun box(): String {
    val ref = ::фу
    assertEquals(528, ref(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32))
    return "OK"
}