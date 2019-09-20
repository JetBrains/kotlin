// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.configuration;

import com.google.common.collect.ImmutableMap;
import com.intellij.execution.configurations.GeneralCommandLine;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds environment variables configuration:
 * <ul>
 * <li>list of user-defined environment variables</li>
 * <li>boolean flag - whether to pass system environment</li>
 * </ul>
 * Instances of this class are immutable objects, so it can be safely passed across threads.
 */
public class EnvironmentVariablesData {
  public static final EnvironmentVariablesData DEFAULT = new EnvironmentVariablesData(ImmutableMap.of(), true);

  private static final String ENVS = "envs";
  private static final String PASS_PARENT_ENVS = "pass-parent-envs";
  private static final String ENV = EnvironmentVariablesComponent.ENV;
  private static final String NAME = EnvironmentVariablesComponent.NAME;
  private static final String VALUE = EnvironmentVariablesComponent.VALUE;

  private final ImmutableMap<String, String> myEnvs;
  private final boolean myPassParentEnvs;

  private EnvironmentVariablesData(@NotNull Map<String, String> envs, boolean passParentEnvs) {
    myEnvs = ImmutableMap.copyOf(envs);
    myPassParentEnvs = passParentEnvs;
  }

  /**
   * @return immutable Map instance containing user-defined environment variables (iteration order is reliable user-specified)
   */
  @NotNull
  public Map<String, String> getEnvs() {
    return myEnvs;
  }

  public boolean isPassParentEnvs() {
    return myPassParentEnvs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EnvironmentVariablesData data = (EnvironmentVariablesData)o;
    return myPassParentEnvs == data.myPassParentEnvs && myEnvs.equals(data.myEnvs);
  }

  @Override
  public int hashCode() {
    int result = myEnvs.hashCode();
    result = 31 * result + (myPassParentEnvs ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "envs=" + myEnvs + ", passParentEnvs=" + myPassParentEnvs;
  }

  @NotNull
  public static EnvironmentVariablesData readExternal(@NotNull Element element) {
    Element envsElement = element.getChild(ENVS);
    if (envsElement == null) {
      return DEFAULT;
    }
    Map<String, String> envs = ImmutableMap.of();
    String passParentEnvsStr = envsElement.getAttributeValue(PASS_PARENT_ENVS);
    boolean passParentEnvs = passParentEnvsStr == null || Boolean.parseBoolean(passParentEnvsStr);
    for (Element envElement : envsElement.getChildren(ENV)) {
      String envName = envElement.getAttributeValue(NAME);
      String envValue = envElement.getAttributeValue(VALUE);
      if (envName != null && envValue != null) {
        if (envs.isEmpty()) {
          envs = new LinkedHashMap<>();
        }
        envs.put(envName, envValue);
      }
    }
    return create(envs, passParentEnvs);
  }

  public void writeExternal(@NotNull Element parent) {
    Element envsElement = new Element(ENVS);
    if (!myPassParentEnvs) {
      // Avoid writing pass-parent-envs="true" to minimize changes in xml comparing it to xml written by
      // com.intellij.execution.configuration.EnvironmentVariablesComponent.writeExternal
      envsElement.setAttribute(PASS_PARENT_ENVS, Boolean.FALSE.toString());
    }
    for (Map.Entry<String, String> entry : myEnvs.entrySet()) {
      envsElement.addContent(new Element(ENV).setAttribute(NAME, entry.getKey()).setAttribute(VALUE, entry.getValue()));
    }
    parent.addContent(envsElement);
  }

  public void configureCommandLine(@NotNull GeneralCommandLine commandLine, boolean consoleParentEnvs) {
    commandLine.withParentEnvironmentType(!myPassParentEnvs ? GeneralCommandLine.ParentEnvironmentType.NONE :
                                          consoleParentEnvs ? GeneralCommandLine.ParentEnvironmentType.CONSOLE
                                                            : GeneralCommandLine.ParentEnvironmentType.SYSTEM);
    commandLine.withEnvironment(myEnvs);
  }

  /**
   * @param envs Map instance containing user-defined environment variables
   *             (iteration order should be reliable user-specified, like {@link LinkedHashMap} or {@link ImmutableMap})
   * @param passParentEnvs true if system environment should be passed
   */
  @NotNull
  public static EnvironmentVariablesData create(@NotNull Map<String, String> envs, boolean passParentEnvs) {
    return passParentEnvs && envs.isEmpty() ? DEFAULT : new EnvironmentVariablesData(envs, passParentEnvs);
  }
}