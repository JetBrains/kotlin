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
 * Copyright (C) 1997-2000 Netscape Communications Corporation. All
 * Rights Reserved.
 *
 * Contributor(s):
 *
 * Patrick Beard
 * Norris Boyd
 * Igor Bukanov
 * Brendan Eich
 * Roger Lawrence
 * Mike McCabe
 * Ian D. Stewart
 * Andi Vajda
 * Andrew Wason
 * Kemal Bayram
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

import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * This class represents the runtime context of an executing script.
 *
 * Before executing a script, an instance of Context must be created
 * and associated with the thread that will be executing the script.
 * The Context will be used to store information about the executing
 * of the script such as the call stack. Contexts are associated with
 * the current thread  using the <a href="#enter()">enter()</a> method.<p>
 *
 * The behavior of the execution engine may be altered through methods
 * such as <a href="#setLanguageVersion>setLanguageVersion</a> and
 * <a href="#setErrorReporter>setErrorReporter</a>.<p>
 *
 * Different forms of script execution are supported. Scripts may be
 * evaluated from the source directly, or first compiled and then later
 * executed. Interactive execution is also supported.<p>
 *
 * Some aspects of script execution, such as type conversions and
 * object creation, may be accessed directly through methods of
 * Context.
 */
public class Context {

    /**
     * Create a new Context.
     *
     * Note that the Context must be associated with a thread before
     * it can be used to execute a script.
     */
    public Context() {
        setLanguageVersion(VERSION_DEFAULT);
    }

    /**
     * Get a context associated with the current thread, creating
     * one if need be.
     *
     * The Context stores the execution state of the JavaScript
     * engine, so it is required that the context be entered
     * before execution may begin. Once a thread has entered
     * a Context, then getCurrentContext() may be called to find
     * the context that is associated with the current thread.
     * <p>
     * Calling <code>enter()</code> will
     * return either the Context currently associated with the
     * thread, or will create a new context and associate it
     * with the current thread. Each call to <code>enter()</code>
     * must have a matching call to <code>exit()</code>. For example,
     * <pre>
     *      Context cx = Context.enter();
     *      try {
     *          ...
     *          cx.evaluateString(...);
     *      }
     *      finally { Context.exit(); }
     * </pre>
     * @return a Context associated with the current thread
     */
    public static Context enter() {
        return enter(null);
    }

    /**
     * Get a Context associated with the current thread, using
     * the given Context if need be.
     * <p>
     * The same as <code>enter()</code> except that <code>cx</code>
     * is associated with the current thread and returned if
     * the current thread has no associated context and <code>cx</code>
     * is not associated with any other thread.
     * @param cx a Context to associate with the thread if possible
     * @return a Context associated with the current thread
     */
    public static Context enter(Context cx) {

        Context old = getCurrentContext();

        if (cx == null) {
            if (old != null) {
                cx = old;
            } else {
                cx = new Context();
                setThreadContext(cx);
            }
        } else {
            if (cx.enterCount != 0) {
                // The suplied context must be the context for
                // the current thread if it is already entered
                if (cx != old) {
                    throw new RuntimeException
                        ("Cannot enter Context active on another thread");
                }
            } else {
                if (old != null) {
                    cx = old;
                } else {
                    setThreadContext(cx);
                }
            }
        }

        ++cx.enterCount;

        return cx;
     }

    /**
     * Exit a block of code requiring a Context.
     *
     * Calling <code>exit()</code> will remove the association between
     * the current thread and a Context if the prior call to
     * <code>enter()</code> on this thread newly associated a Context
     * with this thread.
     * Once the current thread no longer has an associated Context,
     * it cannot be used to execute JavaScript until it is again associated
     * with a Context.
     */
    public static void exit() {
        boolean released = false;
        Context cx = getCurrentContext();
        if (cx == null) {
            throw new RuntimeException
                ("Calling Context.exit without previous Context.enter");
        }
        if (Context.check && cx.enterCount < 1) Context.codeBug();
        --cx.enterCount;
        if (cx.enterCount == 0) {
            released = true;
            setThreadContext(null);
        }
    }

    /**
     * Get the current Context.
     *
     * The current Context is per-thread; this method looks up
     * the Context associated with the current thread. <p>
     *
     * @return the Context associated with the current thread, or
     *         null if no context is associated with the current
     *         thread.
     */
    public static Context getCurrentContext() {
        if (threadLocalCx != null) {
            try {
                return (Context)threadLocalGet.invoke(threadLocalCx, (Object[]) null);
            } catch (Exception ex) { }
        }
        Thread t = Thread.currentThread();
        return (Context) threadContexts.get(t);
    }

    @SuppressWarnings("unchecked")
    private static void setThreadContext(Context cx) {
        if (threadLocalCx != null) {
            try {
                threadLocalSet.invoke(threadLocalCx, new Object[] { cx });
                return;
            } catch (Exception ex) { }
        }
        Thread t = Thread.currentThread();
        if (cx != null) {
            threadContexts.put(t, cx);
        } else {
            threadContexts.remove(t);
        }
    }

    /**
     * Language versions
     *
     * All integral values are reserved for future version numbers.
     */

    /**
     * The unknown version.
     */
    public static final int VERSION_UNKNOWN =   -1;

    /**
     * The default version.
     */
    public static final int VERSION_DEFAULT =    0;

    /**
     * JavaScript 1.0
     */
    public static final int VERSION_1_0 =      100;

    /**
     * JavaScript 1.1
     */
    public static final int VERSION_1_1 =      110;

    /**
     * JavaScript 1.2
     */
    public static final int VERSION_1_2 =      120;

    /**
     * JavaScript 1.3
     */
    public static final int VERSION_1_3 =      130;

    /**
     * JavaScript 1.4
     */
    public static final int VERSION_1_4 =      140;

    /**
     * JavaScript 1.5
     */
    public static final int VERSION_1_5 =      150;

    /**
     * Get the current language version.
     * <p>
     * The language version number affects JavaScript semantics as detailed
     * in the overview documentation.
     *
     * @return an integer that is one of VERSION_1_0, VERSION_1_1, etc.
     */
    public int getLanguageVersion() {
       return version;
    }

    /**
     * Set the language version.
     *
     * <p>
     * Setting the language version will affect functions and scripts compiled
     * subsequently. See the overview documentation for version-specific
     * behavior.
     *
     * @param version the version as specified by VERSION_1_0, VERSION_1_1, etc.
     */
    public void setLanguageVersion(int version) {
        this.version = version;
    }

    /**
     * Get the current error reporter.
     */
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    /**
     * Change the current error reporter.
     *
     * @return the previous error reporter
     */
    public ErrorReporter setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;
        return reporter;
    }

    /**
     * Get the current locale.  Returns the default locale if none has
     * been set.
     *
     * @see java.util.Locale
     */

    public Locale getLocale() {
        if (locale == null)
            locale = Locale.getDefault();
        return locale;
    }

    public static void reportWarning(@NotNull String message, @NotNull CodePosition startPosition, @NotNull CodePosition endPosition)
    {
        Context cx = Context.getContext();
        cx.getErrorReporter().warning(message, startPosition, endPosition);
    }

    public static void reportError(@NotNull String message, @NotNull CodePosition startPosition, @NotNull CodePosition endPosition)
    {
        Context cx = getCurrentContext();
        if (cx != null) {
            cx.errorCount++;
            cx.getErrorReporter().error(message, startPosition, endPosition);
        } else {
            throw new EvaluatorException(message);
        }
    }

    /**
     * if hasFeature(FEATURE_NON_ECMA_GET_YEAR) returns true,
     * Date.prototype.getYear subtructs 1900 only if 1900 <= date < 2000
     * in deviation with Ecma B.2.4
     */
    public static final int FEATURE_NON_ECMA_GET_YEAR = 1;

    /**
     * if hasFeature(FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME) returns true,
     * allow 'function <MemberExpression>(...) { ... }' to be syntax sugar for
     * '<MemberExpression> = function(...) { ... }', when <MemberExpression>
     * is not simply identifier.
     * See Ecma-262, section 11.2 for definition of <MemberExpression>
     */
    public static final int FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME = 2;

    /**
     * if hasFeature(RESERVED_KEYWORD_AS_IDENTIFIER) returns true,
     * treat future reserved keyword (see  Ecma-262, section 7.5.3) as ordinary
     * identifiers but warn about this usage
     */
    public static final int FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER = 3;

    /**
     * if hasFeature(FEATURE_TO_STRING_AS_SOURCE) returns true,
     * calling toString on JS objects gives JS source with code to create an
     * object with all enumeratable fields of the original object instead of
     * printing "[object <object-type>]".
     * By default {@link #hasFeature(int)} returns true only if
     * the current JS version is set to {@link #VERSION_1_2}.
     */
    public static final int FEATURE_TO_STRING_AS_SOURCE = 4;

    /**
     * Controls certain aspects of script semantics.
     * Should be overwritten to alter default behavior.
     * @param featureIndex feature index to check
     * @return true if the <code>featureIndex</code> feature is turned on
     * @see #FEATURE_NON_ECMA_GET_YEAR
     * @see #FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME
     * @see #FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER
     * @see #FEATURE_TO_STRING_AS_SOURCE
     */
    public boolean hasFeature(int featureIndex) {
        switch (featureIndex) {
            case FEATURE_NON_ECMA_GET_YEAR:
               /*
                * During the great date rewrite of 1.3, we tried to track the
                * evolving ECMA standard, which then had a definition of
                * getYear which always subtracted 1900.  Which we
                * implemented, not realizing that it was incompatible with
                * the old behavior...  now, rather than thrash the behavior
                * yet again, we've decided to leave it with the - 1900
                * behavior and point people to the getFullYear method.  But
                * we try to protect existing scripts that have specified a
                * version...
                */
                return (version == Context.VERSION_1_0
                        || version == Context.VERSION_1_1
                        || version == Context.VERSION_1_2);

            case FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME:
                return false;

            case FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER:
                return false;

            case FEATURE_TO_STRING_AS_SOURCE:
                return version == VERSION_1_2;
        }
        // It is a bug to call the method with unknown featureIndex
        throw new IllegalArgumentException();
    }

    /********** end of API **********/

    static String getMessage0(String messageId) {
        return getMessage(messageId, null);
    }

    static String getMessage2(String messageId, Object arg1, Object arg2) {
        Object[] arguments = {arg1, arg2};
        return getMessage(messageId, arguments);
    }

    /**
     * Internal method that reports an error for missing calls to
     * enter().
     */
    static Context getContext() {
        Context cx = getCurrentContext();
        if (cx == null) {
            throw new RuntimeException(
                "No Context associated with current Thread");
        }
        return cx;
    }

    static String getMessage(String messageId, Object[] arguments) {
        String formatString;
        try {
            formatString = messages.getString(messageId);
        } catch (MissingResourceException mre) {
            throw new RuntimeException("No message resource found for message property " + messageId);
        }

        MessageFormat formatter = new MessageFormat(formatString);
        return formatter.format(arguments);
    }

    // debug flags
    static final boolean printTrees = true;
    static final boolean printICode = true;

// Rudimentary support for Design-by-Contract
    static void codeBug() {
        throw new RuntimeException("FAILED ASSERTION");
    }

    static final boolean check = true;

    private static MessagesBundle messages = new MessagesBundle();
    private static Hashtable threadContexts = new Hashtable(11);
    private static Object threadLocalCx;
    private static Method threadLocalGet;
    private static Method threadLocalSet;

    int version;
    int errorCount;

    private ErrorReporter errorReporter;
    private Locale locale;
    private boolean generatingDebug;
    private boolean generatingDebugChanged;
    private boolean generatingSource=true;
    private boolean compileFunctionsWithDynamicScopeFlag;
    private int enterCount;
    private Object[] listeners;
    private Hashtable hashtable;
    private ClassLoader applicationClassLoader;

    /**
     * This is the list of names of objects forcing the creation of
     * function activation records.
     */
    private Hashtable activationNames;

    // For instruction counting (interpreter only)
    int instructionCount;
    int instructionThreshold;
}
