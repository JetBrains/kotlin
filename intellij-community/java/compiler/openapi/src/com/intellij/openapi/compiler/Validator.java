/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.compiler;

import com.intellij.util.DeprecatedMethodException;
import org.jetbrains.annotations.NonNls;

/**
 * An interface for compilers which validate something after the compilation finishes. The validators are disabled by default and can be
 * enabled by user in File | Settings | Build, Execution, Deployment | Compiler | Validation. It's better to implement validation as inspection,
 * in that case you can use {@link com.intellij.openapi.compiler.util.InspectionValidator} extension point to allow users run the inspection
 * after a build finishes.
 *
 * <p>
 * The implementation of this class should be registered in plugin.xml:
 * <pre>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;
 * &nbsp;&nbsp;&lt;compiler implementation="qualified-class-name"/&gt;
 * &lt;/extensions&gt;
 * </pre>
 * </p>
 * </p>
 */
public interface Validator extends FileProcessingCompiler {
  /**
   * Returns unique ID which can be used in project configuration files.
   */
  default @NonNls String getId() {
    DeprecatedMethodException.reportDefaultImplementation(getClass(), "getId",
                                                          "The default implementation delegates to 'getDescription' which may be localized but return value of this method must not depend on current localization.");
    return getDescription();
  }
}
