/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.common;

public final class IdentifierPolicy {
    private IdentifierPolicy() {}

    public static boolean isES5IdentifierStart(char c) {
        if (isAllowedLatinLetterOrSpecial(c)) return true;
        if (isNotAllowedSimpleCharacter(c)) return false;

        return isES5IdentifierStartFull(c);
    }

    public static boolean isES5IdentifierPart(char c) {
        if (isAllowedLatinLetterOrSpecial(c)) return true;
        if (isAllowedSimpleDigit(c)) return true;
        if (isNotAllowedSimpleCharacter(c)) return false;

        int charType = Character.getType(c);

        return isES5IdentifierStartFull(c) ||
               charType == Character.NON_SPACING_MARK ||
               charType == Character.COMBINING_SPACING_MARK ||
               charType == Character.DECIMAL_DIGIT_NUMBER ||
               charType == Character.CONNECTOR_PUNCTUATION ||
               c == '\u200C' ||   // Zero-width non-joiner
               c == '\u200D';      // Zero-width joiner
    }

    public static boolean isValidES5Identifier(String id) {
       if (id.isEmpty() || !isES5IdentifierStart(id.charAt(0))) return false;

       for (int i = 1; i < id.length(); i++) {
           char c = id.charAt(i);
           if (!isES5IdentifierPart(c)) return false;
       }

       return true;
    }

    private static boolean isAllowedLatinLetterOrSpecial(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$';
    }

    private static boolean isAllowedSimpleDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isNotAllowedSimpleCharacter(char c) {
        return c == ' ' || c == '<' || c == '>' || c == '-' || c == '?';
    }

    // See ES 5.1 spec: https://www.ecma-international.org/ecma-262/5.1/#sec-7.6
    private static boolean isES5IdentifierStartFull(char c) {
        return Character.isLetter(c) ||   // Lu | Ll | Lt | Lm | Lo
               // Nl which is missing in Character.isLetter, but present in UnicodeLetter in spec
               Character.getType(c) == Character.LETTER_NUMBER;
    }
}
