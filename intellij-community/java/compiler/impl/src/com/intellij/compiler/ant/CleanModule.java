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
package com.intellij.compiler.ant;

import com.intellij.compiler.ant.taskdefs.Delete;
import com.intellij.compiler.ant.taskdefs.Target;
import com.intellij.openapi.compiler.CompilerBundle;

/**
 * @author Eugene Zhuravlev
 */
public class CleanModule extends Target {
  public CleanModule(ModuleChunk chunk) {
    super(BuildProperties.getModuleCleanTargetName(chunk.getName()), null,
          CompilerBundle.message("generated.ant.build.cleanup.module.task.comment"), null);
    final String chunkName = chunk.getName();
    add(new Delete(BuildProperties.propertyRef(BuildProperties.getOutputPathProperty(chunkName))));
    add(new Delete(BuildProperties.propertyRef(BuildProperties.getOutputPathForTestsProperty(chunkName))));
  }
}
