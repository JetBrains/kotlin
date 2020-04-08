/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.output.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.File;

// TODO Remove this class once general multi-module bytecode text tests are implemented.
abstract public class AbstractGenerateNotNullAssertionsTest extends CodegenTestCase {
    @NotNull
    @Override
    protected String getPrefix() {
        return "notNullAssertions";
    }

    private void setUpEnvironment(boolean disableCallAssertions, boolean disableParamAssertions) {
        File[] extraClassPath = javaClassesOutputDirectory != null ? new File[] {javaClassesOutputDirectory} : new File[0];
        CompilerConfiguration configuration =
                KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, extraClassPath);

        configuration.put(JVMConfigurationKeys.DISABLE_CALL_ASSERTIONS, disableCallAssertions);
        configuration.put(JVMConfigurationKeys.DISABLE_PARAM_ASSERTIONS, disableParamAssertions);
        configuration.put(JVMConfigurationKeys.IR, getBackend().isIR());

        myEnvironment =
                KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        myFiles = null;
    }

    private void loadSource(@NotNull String fileName) {
        loadFileByFullPath(KotlinTestUtils.getTestDataPathBase() + "/codegen/" + getPrefix() + "/" + fileName);
    }

    protected void doTestNoAssertionsForKotlinFromBinary(String binaryDependencyFilename, String testFilename) {
        setUpEnvironment(false, true);
        loadSource(binaryDependencyFilename);
        OutputFileCollection outputFiles = generateClassesInFile();
        javaClassesOutputDirectory = new File(FileUtil.getTempDirectory(), "kotlin-classes");
        OutputUtilsKt.writeAllTo(outputFiles, javaClassesOutputDirectory);

        setUpEnvironment(false, true);
        loadSource(testFilename);

        assertNoIntrinsicsMethodIsCalledInMyClasses(false);
    }

    private void assertNoIntrinsicsMethodIsCalledInMyClasses(boolean noClassFileIsAnError) {
        for (KtFile jetFile : myFiles.getPsiFiles()) {
            String fileClassName = JvmFileClassUtil.getFileClassInfoNoResolve(jetFile).getFileClassFqName().asString();
            assertNoIntrinsicsMethodIsCalled(fileClassName, noClassFileIsAnError);
        }
    }

    private void assertNoIntrinsicsMethodIsCalled(String className, boolean noClassFileIsAnError) {
        OutputFileCollection classes = generateClassesInFile();
        OutputFile file = classes.get(className + ".class");
        if (noClassFileIsAnError) {
            assertNotNull("File for " + className + " is absent", file);
        }
        else if (file == null) {
            return;
        }
        ClassReader reader = new ClassReader(file.asByteArray());

        reader.accept(new ClassVisitor(Opcodes.API_VERSION) {
            @Override
            public MethodVisitor visitMethod(
                    int access, @NotNull String callerName, @NotNull String callerDesc, String signature, String[] exceptions
            ) {
                return new MethodVisitor(Opcodes.API_VERSION) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        assertFalse(
                                "Intrinsics method is called: " + name + desc + "  Caller: " + callerName + callerDesc,
                                "kotlin/jvm/internal/Intrinsics".equals(owner)
                        );
                    }
                };
            }
        }, 0);
    }
}
