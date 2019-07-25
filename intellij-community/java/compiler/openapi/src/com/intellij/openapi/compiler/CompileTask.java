// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.compiler;

/**
 * Describes a task to be executed before or after compilation in the IDE process. The implementation should be registered in plugin.xml file:
 *
 * <pre>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;
 * &nbsp;&nbsp;&lt;compiler.task execute="BEFORE" implementation="qualified-class-name"/&gt;
 * &lt;/extensions&gt;
 * </pre>
 * or
 * <pre>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;
 * &nbsp;&nbsp;&lt;compiler.task execute="AFTER" implementation="qualified-class-name"/&gt;
 * &lt;/extensions&gt;
 * </pre>
 *
 */
public interface CompileTask {
  /**
   * Executes the task.
   *
   * @param context current compile context
   * @return true if execution succeeded, false otherwise. If the task returns false, the compilation
   *         is aborted, and it's expected that the task adds a message defining the reason for the failure
   *         to the compile context.
   */
  boolean execute(CompileContext context);
}
