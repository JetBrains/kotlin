/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * The contents of this file are subject to the Netscape Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/NPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is Netscape
 * Communications Corporation.  Portions created by Netscape are
 * Copyright (C) 1997-1999 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s):
 * Waldemar Horwat
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the GNU Public License (the "GPL"), in which case the
 * provisions of the GPL are applicable instead of those above.
 * If you wish to allow use of your version of this file only
 * under the terms of the GPL and not to allow others to use your
 * version of this file under the NPL, indicate your decision by
 * deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL.  If you do not delete
 * the provisions above, a recipient may use your version of this
 * file under either the NPL or the GPL.
 */
// Modified by Google

package com.google.gwt.dev.js.rhino;

final class BinaryDigitReader {
    int lgBase;         // Logarithm of base of number
    int digit;          // Current digit value in radix given by base
    int digitPos;       // Bit position of last bit extracted from digit
    String digits;      // String containing the digits
    int start;          // Index of the first remaining digit
    int end;            // Index past the last remaining digit

    BinaryDigitReader(int base, String digits, int start, int end) {
        lgBase = 0;
        while (base != 1) {
            lgBase++;
            base >>= 1;
        }
        digitPos = 0;
        this.digits = digits;
        this.start = start;
        this.end = end;
    }

    /* Return the next binary digit from the number or -1 if done */
    int getNextBinaryDigit()
    {
        if (digitPos == 0) {
            if (start == end)
                return -1;

            char c = digits.charAt(start++);
            if ('0' <= c && c <= '9')
                digit = c - '0';
            else if ('a' <= c && c <= 'z')
                digit = c - 'a' + 10;
            else digit = c - 'A' + 10;
            digitPos = lgBase;
        }
        return digit >> --digitPos & 1;
    }
}
