// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.compiler;

import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Provides a way to specify file types, which can be processed by a build process. This knowledge allows the IDE to treat files of these types
 * accordingly: for example, this enables "Compile" action on such files in a context menu.
 * The implementation should be registered in plugin.xml file:
 *
 * <pre>
 * &lt;extensions defaultExtensionNs="com.intellij"&gt;
 * &nbsp;&nbsp;&lt;compilableFileTypesProvider implementation="com.mycompany.compiler.MyTypesProvider"/&gt;
 * &lt;/extensions&gt;
 * </pre>
 *
 * @see CompilerManager#isCompilableFileType
 */
public interface CompilableFileTypesProvider {
  @NotNull
  Set<FileType> getCompilableFileTypes();
}
