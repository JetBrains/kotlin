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
package com.intellij.compiler.ant.artifacts;

import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.compiler.ant.taskdefs.Delete;
import com.intellij.compiler.ant.BuildProperties;
import com.intellij.packaging.artifacts.Artifact;

/**
 * @author nik
 */
public class CleanArtifactTarget extends Target {
  public CleanArtifactTarget(Artifact artifact, ArtifactAntGenerationContextImpl context) {
    super(context.getCleanTargetName(artifact), null, "clean " + artifact.getName() + " artifact output", null);
    add(new Delete(BuildProperties.propertyRef(context.getConfiguredArtifactOutputProperty(artifact))));
  }
}
