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
 * Norris Boyd
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

// API class

package org.jetbrains.kotlin.js.parser;

import org.jetbrains.annotations.NotNull;

/**
 * This is interface defines a protocol for the reporting of
 * errors during JavaScript translation or execution.
 */

public interface ErrorReporter {

    /**
     * Report a warning.
     *
     * The implementing class may choose to ignore the warning
     * if it desires.
     *
     * @param message a String describing the error
     * @param startPosition position before error token
     * @param endPosition position after error token
     */
    void warning(@NotNull String message, @NotNull CodePosition startPosition, @NotNull CodePosition endPosition);

    /**
     * Report an error.
     *
     * The implementing class is free to throw an exception if
     * it desires.
     *
     * If execution has not yet begun, the JavaScript engine is
     * free to find additional errors rather than terminating
     * the translation. It will not execute a script that had
     * errors, however.
     *
     * @param message a String describing the error
     * @param startPosition position before error token
     * @param endPosition position after error token
     */
    void error(@NotNull String message, @NotNull CodePosition startPosition, @NotNull CodePosition endPosition);
}
