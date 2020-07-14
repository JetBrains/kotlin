/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.test;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.pom.java.LanguageLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class KotlinWithJdkAndRuntimeLightProjectDescriptor extends KotlinJdkAndLibraryProjectDescriptor {
    protected KotlinWithJdkAndRuntimeLightProjectDescriptor() {
        super(KotlinArtifacts.getInstance().getKotlinStdlib());
    }

    public KotlinWithJdkAndRuntimeLightProjectDescriptor(@NotNull List<? extends File> libraryFiles) {
        super(libraryFiles);
    }

    @NotNull
    public static final KotlinWithJdkAndRuntimeLightProjectDescriptor INSTANCE = new KotlinWithJdkAndRuntimeLightProjectDescriptor(
            Arrays.asList(KotlinArtifacts.getInstance().getKotlinStdlib(),
                          ForTestCompileRuntime.coroutinesCompatForTests())
    );

    public static KotlinWithJdkAndRuntimeLightProjectDescriptor getInstance(LanguageLevel level) {
        return new KotlinWithJdkAndRuntimeLightProjectDescriptor() {
            @Override
            public void configureModule(
                    @NotNull Module module, @NotNull ModifiableRootModel model
            ) {
                super.configureModule(module, model);
                model.getModuleExtension(LanguageLevelModuleExtension.class).setLanguageLevel(level);
            }
        };
    }

    @NotNull
    public static final KotlinWithJdkAndRuntimeLightProjectDescriptor INSTANCE_WITH_KOTLIN_TEST = new KotlinWithJdkAndRuntimeLightProjectDescriptor(
            Arrays.asList(KotlinArtifacts.getInstance().getKotlinStdlib(),
                          KotlinArtifacts.getInstance().getKotlinTest())
    );

    @NotNull
    public static final KotlinWithJdkAndRuntimeLightProjectDescriptor INSTANCE_WITH_SCRIPT_RUNTIME = new KotlinWithJdkAndRuntimeLightProjectDescriptor(

            Arrays.asList(KotlinArtifacts.getInstance().getKotlinStdlib(),
                          KotlinArtifacts.getInstance().getKotlinScriptRuntime())
    );

    @NotNull
    public static final KotlinWithJdkAndRuntimeLightProjectDescriptor INSTANCE_WITH_REFLECT = new KotlinWithJdkAndRuntimeLightProjectDescriptor(
            Arrays.asList(KotlinArtifacts.getInstance().getKotlinStdlib(),
                          KotlinArtifacts.getInstance().getKotlinReflect())
    );

    @NotNull
    public static final KotlinWithJdkAndRuntimeLightProjectDescriptor INSTANCE_FULL_JDK = new KotlinWithJdkAndRuntimeLightProjectDescriptor() {
        @Override
        public Sdk getSdk() {
            return PluginTestCaseBase.fullJdk();
        }
    };
}
