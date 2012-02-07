package org.jetbrains.k2js.test;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * @author Pavel Talanov
 */
public interface RhinoResultChecker {
    public void runChecks(Context context, Scriptable scope) throws Exception;
}
