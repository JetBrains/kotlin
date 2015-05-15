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

package org.jetbrains.kotlin.compilerRunner;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.ExitCode;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil;
import org.jetbrains.kotlin.config.CompilerSettings;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR;
import static org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO;

public class KotlinCompilerRunner {
    private static final String K2JVM_COMPILER = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler";
    private static final String K2JS_COMPILER = "org.jetbrains.kotlin.cli.js.K2JSCompiler";
    private static final String INTERNAL_ERROR = ExitCode.INTERNAL_ERROR.toString();

    public static void runK2JvmCompiler(
            CommonCompilerArguments commonArguments,
            K2JVMCompilerArguments k2jvmArguments,
            CompilerSettings compilerSettings,
            MessageCollector messageCollector,
            CompilerEnvironment environment,
            File moduleFile,
            OutputItemsCollector collector
    ) {
        K2JVMCompilerArguments arguments = mergeBeans(commonArguments, k2jvmArguments);
        setupK2JvmArguments(moduleFile, arguments);

        runCompiler(K2JVM_COMPILER, arguments, compilerSettings.getAdditionalArguments(), messageCollector, collector, environment);
    }

    public static void runK2JsCompiler(
            @NotNull CommonCompilerArguments commonArguments,
            @NotNull K2JSCompilerArguments k2jsArguments,
            @NotNull CompilerSettings compilerSettings,
            @NotNull MessageCollector messageCollector,
            @NotNull CompilerEnvironment environment,
            @NotNull OutputItemsCollector collector,
            @NotNull Collection<File> sourceFiles,
            @NotNull List<String> libraryFiles,
            @NotNull File outputFile
    ) {
        K2JSCompilerArguments arguments = mergeBeans(commonArguments, k2jsArguments);
        setupK2JsArguments(outputFile, sourceFiles, libraryFiles, arguments);

        runCompiler(K2JS_COMPILER, arguments, compilerSettings.getAdditionalArguments(), messageCollector, collector, environment);
    }

    private static void runCompiler(
            String compilerClassName,
            CommonCompilerArguments arguments,
            String additionalArguments,
            MessageCollector messageCollector,
            OutputItemsCollector collector,
            CompilerEnvironment environment
    ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(stream);

        String exitCode = execCompiler(compilerClassName, arguments, additionalArguments, environment, out, messageCollector);

        BufferedReader reader = new BufferedReader(new StringReader(stream.toString()));
        CompilerOutputParser.parseCompilerMessagesFromReader(messageCollector, reader, collector);

        if (INTERNAL_ERROR.equals(exitCode)) {
            messageCollector.report(ERROR, "Compiler terminated with internal error", NO_LOCATION);
        }
    }

    @NotNull
    private static String execCompiler(
            String compilerClassName,
            CommonCompilerArguments arguments,
            String additionalArguments,
            CompilerEnvironment environment,
            PrintStream out,
            MessageCollector messageCollector
    ) {
        try {
            messageCollector.report(INFO, "Using kotlin-home = " + environment.getKotlinPaths().getHomePath(), NO_LOCATION);

            List<String> argumentsList = ArgumentUtils.convertArgumentsToStringList(arguments);
            argumentsList.addAll(StringUtil.split(additionalArguments, " "));

            Object rc = CompilerRunnerUtil.invokeExecMethod(
                    compilerClassName, ArrayUtil.toStringArray(argumentsList), environment, messageCollector, out
            );

            // exec() returns an ExitCode object, class of which is loaded with a different class loader,
            // so we take it's contents through reflection
            return getReturnCodeFromObject(rc);
        }
        catch (Throwable e) {
            MessageCollectorUtil.reportException(messageCollector, e);
            return INTERNAL_ERROR;
        }
    }

    @NotNull
    private static String getReturnCodeFromObject(@Nullable Object rc) throws Exception {
        if (rc == null) {
            return INTERNAL_ERROR;
        }
        else if (ExitCode.class.getName().equals(rc.getClass().getName())) {
            return rc.toString();
        }
        else {
            throw new IllegalStateException("Unexpected return: " + rc);
        }
    }

    private static <T extends CommonCompilerArguments> T mergeBeans(CommonCompilerArguments from, T to) {
        // TODO: rewrite when updated version of com.intellij.util.xmlb is available on TeamCity
        try {
            T copy = XmlSerializerUtil.createCopy(to);

            List<Field> fromFields = collectFieldsToCopy(from.getClass());
            for (Field fromField : fromFields) {
                Field toField = copy.getClass().getField(fromField.getName());
                toField.set(copy, fromField.get(from));
            }

            return copy;
        }
        catch (NoSuchFieldException e) {
            throw UtilsPackage.rethrow(e);
        }
        catch (IllegalAccessException e) {
            throw UtilsPackage.rethrow(e);
        }
    }

    private static List<Field> collectFieldsToCopy(Class<?> clazz) {
        List<Field> fromFields = new ArrayList<Field>();

        Class currentClass = clazz;
        do {
            for (Field field : currentClass.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers)) {
                    fromFields.add(field);
                }
            }
        }
        while ((currentClass = currentClass.getSuperclass()) != null);

        return fromFields;
    }

    private static void setupK2JvmArguments(File moduleFile, K2JVMCompilerArguments settings) {
        settings.module = moduleFile.getAbsolutePath();
        settings.noStdlib = true;
        settings.noJdkAnnotations = true;
        settings.noJdk = true;
    }

    private static void setupK2JsArguments(
            @NotNull File outputFile,
            @NotNull Collection<File> sourceFiles,
            @NotNull List<String> libraryFiles,
            @NotNull K2JSCompilerArguments settings
    ) {
        settings.noStdlib = true;
        settings.freeArgs = ContainerUtil.map(sourceFiles, new Function<File, String>() {
            @Override
            public String fun(File file) {
                return file.getPath();
            }
        });
        settings.outputFile = outputFile.getPath();
        settings.metaInfo = true;
        settings.libraryFiles = ArrayUtil.toStringArray(libraryFiles);
    }
}
