/*
 * Copyright 2010-2020 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.idea.reporter.KotlinReportSubmitter;
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm;
import org.jetbrains.kotlin.resolve.konan.diagnostics.ErrorsNative;

public class PluginStartupActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(PluginStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        StartupCompatKt.runActivity(project);
        PluginStartupService.Companion.getInstance(project).register(project);

        initializeDiagnostics();

        try {
            // API added in 15.0.2
            UpdateChecker.INSTANCE.getExcludedFromUpdateCheckPlugins().add("org.jetbrains.kotlin");
        }
        catch (Throwable throwable) {
            LOG.debug("Excluding Kotlin plugin updates using old API", throwable);
            //UpdateChecker.getDisabledToUpdate().add(PluginId.getId("org.jetbrains.kotlin"));
            UpdateChecker.getDisabledToUpdatePlugins().add("org.jetbrains.kotlin");
        }

        KotlinPluginCompatibilityVerifier.checkCompatibility();

        KotlinReportSubmitter.Companion.setupReportingFromRelease();

        //todo[Sedunov]: wait for fix in platform to avoid misunderstood from Java newbies (also ConfigureKotlinInTempDirTest)
        //KotlinSdkType.Companion.setUpIfNeeded();
    }

    /*
        Concurrent access to Errors may lead to the class loading dead lock because of non-trivial initialization in Errors.
        As a work-around, all Error classes are initialized beforehand.
        It doesn't matter what exact diagnostic factories are used here.
     */
    private static void initializeDiagnostics() {
        consumeFactory(Errors.DEPRECATION);
        consumeFactory(ErrorsJvm.ACCIDENTAL_OVERRIDE);
        consumeFactory(ErrorsJs.CALL_FROM_UMD_MUST_BE_JS_MODULE_AND_JS_NON_MODULE);
        consumeFactory(ErrorsNative.INCOMPATIBLE_THROWS_INHERITED);
    }

    private static void consumeFactory(DiagnosticFactory<?> factory) {
        //noinspection ResultOfMethodCallIgnored
        factory.getClass();
    }

}
