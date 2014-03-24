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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.ResourceBundle;

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
 *
 * @see Scriptable
 * @author Norris Boyd
 * @author Brendan Eich
 */

public class Context {
    public static final String languageVersionProperty = "language version";
    public static final String errorReporterProperty   = "error reporter";

    /**
     * Create a new Context.
     *
     * Note that the Context must be associated with a thread before
     * it can be used to execute a script.
     *
     * @see org.mozilla.javascript.Context#enter
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
     * @see org.mozilla.javascript.Context#getCurrentContext
     * @see org.mozilla.javascript.Context#exit
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
     *
     * @see org.mozilla.javascript.Context#enter
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
     * @see org.mozilla.javascript.Context#enter
     * @see org.mozilla.javascript.Context#exit
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
     * Get the implementation version.
     *
     * <p>
     * The implementation version is of the form
     * <pre>
     *    "<i>name langVer</i> <code>release</code> <i>relNum date</i>"
     * </pre>
     * where <i>name</i> is the name of the product, <i>langVer</i> is
     * the language version, <i>relNum</i> is the release number, and
     * <i>date</i> is the release date for that specific
     * release in the form "yyyy mm dd".
     *
     * @return a string that encodes the product, language version, release
     *         number, and date.
     */
     public String getImplementationVersion() {
        return "Rhino 1.5 release 4.1 2003 04 21";
     }

    /**
     * Get the current error reporter.
     *
     * @see org.mozilla.javascript.ErrorReporter
     */
    public ErrorReporter getErrorReporter() {
        return errorReporter;
    }

    /**
     * Change the current error reporter.
     *
     * @return the previous error reporter
     * @see org.mozilla.javascript.ErrorReporter
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

    /**
     * Set the current locale.
     *
     * @see java.util.Locale
     */
    public Locale setLocale(Locale loc) {
        Locale result = locale;
        locale = loc;
        return result;
    }

    /**
     * Notify any registered listeners that a bounded property has changed
     * @see #addPropertyChangeListener(java.beans.PropertyChangeListener)
     * @see #removePropertyChangeListener(java.beans.PropertyChangeListener)
     * @see java.beans.PropertyChangeListener
     * @see java.beans.PropertyChangeEvent
     * @param  property  the bound property
     * @param  oldValue  the old value
     * @param  newVale   the new value
     */
    void firePropertyChange(String property, Object oldValue,
                            Object newValue)
    {
        Object[] array = listeners;
        if (array != null) {
            firePropertyChangeImpl(array, property, oldValue, newValue);
        }
    }

    private void firePropertyChangeImpl(Object[] array, String property,
                                        Object oldValue, Object newValue)
    {
        for (int i = array.length; i-- != 0;) {
            Object obj = array[i];
            if (obj instanceof PropertyChangeListener) {
                PropertyChangeListener l = (PropertyChangeListener)obj;
                l.propertyChange(new PropertyChangeEvent(
                    this, property, oldValue, newValue));
            }
    }
    }

    /**
     * Report a warning using the error reporter for the current thread.
     *
     * @param message the warning message to report
     * @param sourceName a string describing the source, such as a filename
     * @param lineno the starting line number
     * @param lineSource the text of the line (may be null)
     * @param lineOffset the offset into lineSource where problem was detected
     * @see org.mozilla.javascript.ErrorReporter
     */
    public static void reportWarning(String message, String sourceName,
                                     int lineno, String lineSource,
                                     int lineOffset)
    {
        Context cx = Context.getContext();
        cx.getErrorReporter().warning(message, sourceName, lineno,
                                      lineSource, lineOffset);
    }

    /**
     * Report a warning using the error reporter for the current thread.
     *
     * @param message the warning message to report
     * @see org.mozilla.javascript.ErrorReporter
     */
    /*
    public static void reportWarning(String message) {
        int[] linep = { 0 };
        String filename = getSourcePositionFromStack(linep);
        Context.reportWarning(message, filename, linep[0], null, 0);
    }
    */

    /**
     * Report an error using the error reporter for the current thread.
     *
     * @param message the error message to report
     * @param sourceName a string describing the source, such as a filename
     * @param lineno the starting line number
     * @param lineSource the text of the line (may be null)
     * @param lineOffset the offset into lineSource where problem was detected
     * @see org.mozilla.javascript.ErrorReporter
     */
    public static void reportError(String message, String sourceName,
                                   int lineno, String lineSource,
                                   int lineOffset)
    {
        Context cx = getCurrentContext();
        if (cx != null) {
            cx.errorCount++;
            cx.getErrorReporter().error(message, sourceName, lineno,
                                        lineSource, lineOffset);
        } else {
            throw new EvaluatorException(message);
        }
    }

    /**
     * Report an error using the error reporter for the current thread.
     *
     * @param message the error message to report
     * @see org.mozilla.javascript.ErrorReporter
     */
    /*
    public static void reportError(String message) {
        int[] linep = { 0 };
        String filename = getSourcePositionFromStack(linep);
        Context.reportError(message, filename, linep[0], null, 0);
    }
    */

    /**
     * Report a runtime error using the error reporter for the current thread.
     *
     * @param message the error message to report
     * @param sourceName a string describing the source, such as a filename
     * @param lineno the starting line number
     * @param lineSource the text of the line (may be null)
     * @param lineOffset the offset into lineSource where problem was detected
     * @return a runtime exception that will be thrown to terminate the
     *         execution of the script
     * @see org.mozilla.javascript.ErrorReporter
     */
    /*
    public static EvaluatorException reportRuntimeError(String message,
                                                      String sourceName,
                                                      int lineno,
                                                      String lineSource,
                                                      int lineOffset)
    {
        Context cx = getCurrentContext();
        if (cx != null) {
            cx.errorCount++;
            return cx.getErrorReporter().
                            runtimeError(message, sourceName, lineno,
                                         lineSource, lineOffset);
        } else {
            throw new EvaluatorException(message);
        }
    }

    static EvaluatorException reportRuntimeError0(String messageId) {
        return reportRuntimeError(getMessage0(messageId));
    }

    static EvaluatorException reportRuntimeError1
        (String messageId, Object arg1)
    {
        return reportRuntimeError(getMessage1(messageId, arg1));
    }

    static EvaluatorException reportRuntimeError2
        (String messageId, Object arg1, Object arg2)
    {
        return reportRuntimeError(getMessage2(messageId, arg1, arg2));
    }

    static EvaluatorException reportRuntimeError3
        (String messageId, Object arg1, Object arg2, Object arg3)
    {
        return reportRuntimeError(getMessage3(messageId, arg1, arg2, arg3));
    }
    */

    /**
     * Report a runtime error using the error reporter for the current thread.
     *
     * @param message the error message to report
     * @see org.mozilla.javascript.ErrorReporter
     */
    /*
    public static EvaluatorException reportRuntimeError(String message) {
        int[] linep = { 0 };
        String filename = getSourcePositionFromStack(linep);
        return Context.reportRuntimeError(message, filename, linep[0], null, 0);
    }
    */

    /**
     * Get a value corresponding to a key.
     * <p>
     * Since the Context is associated with a thread it can be
     * used to maintain values that can be later retrieved using
     * the current thread.
     * <p>
     * Note that the values are maintained with the Context, so
     * if the Context is disassociated from the thread the values
     * cannot be retreived. Also, if private data is to be maintained
     * in this manner the key should be a java.lang.Object
     * whose reference is not divulged to untrusted code.
     * @param key the key used to lookup the value
     * @return a value previously stored using putThreadLocal.
     */
    public final Object getThreadLocal(Object key) {
        if (hashtable == null)
            return null;
        return hashtable.get(key);
    }

    /**
     * Put a value that can later be retrieved using a given key.
     * <p>
     * @param key the key used to index the value
     * @param value the value to save
     */
    public void putThreadLocal(Object key, Object value) {
        if (hashtable == null)
            hashtable = new Hashtable();
        hashtable.put(key, value);
    }

    /**
     * Remove values from thread-local storage.
     * @param key the key for the entry to remove.
     * @since 1.5 release 2
     */
    public void removeThreadLocal(Object key) {
        if (hashtable == null)
            return;
        hashtable.remove(key);
    }

    /**
     * Return whether functions are compiled by this context using
     * dynamic scope.
     * <p>
     * If functions are compiled with dynamic scope, then they execute
     * in the scope of their caller, rather than in their parent scope.
     * This is useful for sharing functions across multiple scopes.
     * @since 1.5 Release 1
     */
    public final boolean hasCompileFunctionsWithDynamicScope() {
        return compileFunctionsWithDynamicScopeFlag;
    }

    /**
     * Set whether functions compiled by this context should use
     * dynamic scope.
     * <p>
     * @param flag if true, compile functions with dynamic scope
     * @since 1.5 Release 1
     */
    public void setCompileFunctionsWithDynamicScope(boolean flag) {
        compileFunctionsWithDynamicScopeFlag = flag;
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

    static String getMessage1(String messageId, Object arg1) {
        Object[] arguments = {arg1};
        return getMessage(messageId, arguments);
    }

    static String getMessage2(String messageId, Object arg1, Object arg2) {
        Object[] arguments = {arg1, arg2};
        return getMessage(messageId, arguments);
    }

    static String getMessage3
        (String messageId, Object arg1, Object arg2, Object arg3) {
        Object[] arguments = {arg1, arg2, arg3};
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

    /* OPT there's a noticable delay for the first error!  Maybe it'd
     * make sense to use a ListResourceBundle instead of a properties
     * file to avoid (synchronized) text parsing.
     */
    // bruce: removed referenced to the initial "java" package name
    //        that used to be there due to a build artifact 
    static final String defaultResource =
      "com.google.gwt.dev.js.rhino.Messages";
    

    static String getMessage(String messageId, Object[] arguments) {
        Context cx = getCurrentContext();
        Locale locale = cx != null ? cx.getLocale() : Locale.getDefault();

        // ResourceBundle does cacheing.
        ResourceBundle rb = ResourceBundle.getBundle(defaultResource, locale);

        String formatString;
        try {
            formatString = rb.getString(messageId);
        } catch (java.util.MissingResourceException mre) {
            throw new RuntimeException
                ("no message resource found for message property "+ messageId);
        }

        /*
         * It's OK to format the string, even if 'arguments' is null;
         * we need to format it anyway, to make double ''s collapse to
         * single 's.
         */
        // TODO: MessageFormat is not available on pJava
        MessageFormat formatter = new MessageFormat(formatString);
        return formatter.format(arguments);
    }

    // debug flags
    static final boolean printTrees = true;
    static final boolean printICode = true;

    final boolean isVersionECMA1() {
        return version == VERSION_DEFAULT || version >= VERSION_1_3;
    }


// Rudimentary support for Design-by-Contract
    static void codeBug() {
        throw new RuntimeException("FAILED ASSERTION");
    }

    static final boolean check = true;

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
