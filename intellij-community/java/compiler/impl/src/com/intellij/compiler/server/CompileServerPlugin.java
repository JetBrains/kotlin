// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.server;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.PluginAware;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

/**
 * This is the preferred way to specify compile server classpath. If it's not flexible enough for you, consider using
 * {@link BuildProcessParametersProvider}
 */
public class CompileServerPlugin implements PluginAware {
  public static final ExtensionPointName<CompileServerPlugin> EP_NAME = ExtensionPointName.create("com.intellij.compileServer.plugin");

  private PluginDescriptor myPluginDescriptor;
  private String myClasspath;

  /**
   * <p>Specifies semicolon-separated list of paths which should be added to the classpath of the compile server.
   * The paths are relative to the plugin 'lib' directory.</p>
   *
   * <p>In the development mode the name of each file without extension is treated as a module name and the output directory of the module
   * is added to the classpath. If such file doesn't exists the jar is searched under 'lib' directory of the plugin sources home directory.</p>
   */
  @Attribute("classpath")
  public String getClasspath() {
    return myClasspath;
  }

  @SuppressWarnings("UnusedDeclaration")
  public void setClasspath(String classpath) {
    myClasspath = classpath;
  }

  @Transient
  public PluginDescriptor getPluginDescriptor() {
    return myPluginDescriptor;
  }

  @Override
  public final void setPluginDescriptor(@NotNull PluginDescriptor pluginDescriptor) {
    myPluginDescriptor = pluginDescriptor;
  }
}
