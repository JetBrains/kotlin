/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package org.jetbrains.jps.model.jarRepository.impl;

import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementChildRole;
import org.jetbrains.jps.model.ex.JpsElementBase;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.jarRepository.JpsRemoteRepositoriesConfiguration;
import org.jetbrains.jps.model.jarRepository.JpsRemoteRepositoryDescription;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class JpsRemoteRepositoriesConfigurationImpl extends JpsElementBase<JpsRemoteRepositoriesConfigurationImpl> implements JpsRemoteRepositoriesConfiguration{
  public static final JpsElementChildRole<JpsRemoteRepositoriesConfiguration> ROLE = JpsElementChildRoleBase.create("remote repositories configuration");
  
  private final List<JpsRemoteRepositoryDescription> myRepositories = new SmartList<>();

  public JpsRemoteRepositoriesConfigurationImpl() {
    this(Arrays.asList( // defaults
      new JpsRemoteRepositoryDescription("central", "Maven Central repository", "https://repo1.maven.org/maven2"),
      new JpsRemoteRepositoryDescription("jboss.community", "JBoss Community repository", "https://repository.jboss.org/nexus/content/repositories/public/")
    ));
  }

  public JpsRemoteRepositoriesConfigurationImpl(List<? extends JpsRemoteRepositoryDescription> repositories) {
    myRepositories.addAll(repositories);
  }

  @NotNull
  @Override
  public JpsRemoteRepositoriesConfigurationImpl createCopy() {
    return new JpsRemoteRepositoriesConfigurationImpl(myRepositories);
  }

  @Override
  public void applyChanges(@NotNull JpsRemoteRepositoriesConfigurationImpl modified) {
    setRepositories(modified.getRepositories());
  }

  @Override
  public List<JpsRemoteRepositoryDescription> getRepositories() {
    return Collections.unmodifiableList(myRepositories);
  }

  @Override
  public void setRepositories(List<? extends JpsRemoteRepositoryDescription> repositories) {
    myRepositories.clear();
    myRepositories.addAll(repositories);
  }
}
