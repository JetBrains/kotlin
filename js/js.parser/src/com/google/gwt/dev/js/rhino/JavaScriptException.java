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

package com.google.gwt.dev.js.rhino;


/**
 * Java reflection of JavaScript exceptions.  (Possibly wrapping a Java exception.)
 *
 * @author Mike McCabe
 */
public class JavaScriptException extends Exception {

    /**
     * Create a JavaScript exception wrapping the given JavaScript value.
     *
     * Instances of this class are thrown by the JavaScript 'throw' keyword.
     *
     * @param value the JavaScript value thrown.
     */
    public JavaScriptException(Object value) {
        super(value.toString());
        this.value = value;
    }

    /**
     * Get the exception value originally thrown.  This may be a
     * JavaScript value (null, undefined, Boolean, Number, String,
     * Scriptable or Function) or a Java exception value thrown from a
     * host object or from Java called through LiveConnect.
     *
     * @return the value wrapped by this exception
     */
    public Object getValue() {
        return value;
    }

    /**
     * The JavaScript exception value.  This value is not
     * intended for general use; if the JavaScriptException wraps a
     * Java exception, getScriptableValue may return a Scriptable
     * wrapping the original Java exception object.
     *
     * We would prefer to go through a getter to encapsulate the value,
     * however that causes the bizarre error "nanosecond timeout value
     * out of range" on the MS JVM.
     * @serial
     */
    Object value;
}
