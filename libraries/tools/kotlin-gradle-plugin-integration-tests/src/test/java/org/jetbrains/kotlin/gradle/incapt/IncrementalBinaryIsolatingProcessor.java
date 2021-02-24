/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incapt;

import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

import static org.jetbrains.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.jetbrains.org.objectweb.asm.Opcodes.ACC_SUPER;

/** Simple processor that generates a class for every annotated element (class, field, method). */
public class IncrementalBinaryIsolatingProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("example.ExampleAnnotation");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) return true;

        for (Element element : roundEnv.getElementsAnnotatedWith(annotations.iterator().next())) {
            if (element instanceof TypeElement || element instanceof ExecutableElement || element instanceof VariableElement) {
                String name = element.getSimpleName().toString();
                name = name.substring(0, 1).toUpperCase() + name.substring(1) + "Generated";
                System.out.println("kapt: IncrementalBinaryIsolatingProcessor "  + name);
                String packageName;
                if (element instanceof TypeElement) {
                    packageName = element.getEnclosingElement().getSimpleName().toString();
                }
                else {
                    packageName = element.getEnclosingElement().getEnclosingElement().getSimpleName().toString();
                }

                String generatedClassName = packageName + "." + name;
                try (OutputStream stream = processingEnv.getFiler().createClassFile(generatedClassName, element).openOutputStream()) {
                    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    writer.visit(Opcodes.V1_8,
                                 ACC_PUBLIC | ACC_SUPER,
                                 generatedClassName.replaceAll("\\.", "/"),
                                 null,
                                 "java/lang/Object",
                                 null);

                    writer.visitAnnotation(Type.getObjectType("example/KotlinFilerGenerated").getDescriptor(), true);
                    writer.visitEnd();
                    stream.write(writer.toByteArray());
                }
                catch (IOException ignored) {
                }
            }
        }

        return false;
    }
}