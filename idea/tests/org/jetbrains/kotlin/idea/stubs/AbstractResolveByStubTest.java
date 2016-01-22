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

package org.jetbrains.kotlin.idea.stubs;

import com.intellij.openapi.util.io.FileUtil;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.test.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator;
import org.junit.Assert;

import java.io.File;

import static org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden;

public abstract class AbstractResolveByStubTest extends KotlinCodeInsightTestCase {
    protected void doTest(String testFileName) throws Exception {
        doTest(testFileName, true, true);
    }

    private void doTest(@NotNull final String path, final boolean checkPrimaryConstructors, final boolean checkPropertyAccessors)
            throws Exception {
        configureByFile(path);
        TestUtilsKt.configureAs(getModule(), KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE);
        boolean shouldFail = getTestName(false).equals("ClassWithConstVal");
        AstAccessControl.INSTANCE.testWithControlledAccessToAst(
                shouldFail, getProject(), getTestRootDisposable(),
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        performTest(path, checkPrimaryConstructors, checkPropertyAccessors);
                        return Unit.INSTANCE;
                    }
                }
        );
    }

    private void performTest(@NotNull String path, boolean checkPrimaryConstructors, boolean checkPropertyAccessors) {
        KtFile file = (KtFile) getFile();
        ModuleDescriptor module = ResolutionUtils.findModuleDescriptor(file);
        PackageViewDescriptor packageViewDescriptor = module.getPackage(new FqName("test"));
        Assert.assertFalse(packageViewDescriptor.isEmpty());

        File fileToCompareTo = new File(FileUtil.getNameWithoutExtension(path) + ".txt");

        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(
                packageViewDescriptor,
                RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT
                        .filterRecursion(RecursiveDescriptorComparator.SKIP_BUILT_INS_PACKAGES)
                        .checkPrimaryConstructors(checkPrimaryConstructors)
                        .checkPropertyAccessors(checkPropertyAccessors)
                        .withValidationStrategy(errorTypesForbidden()),
                fileToCompareTo
        );
    }

    @Override
    protected String getTestDataPath() {
        return "";
    }
}
