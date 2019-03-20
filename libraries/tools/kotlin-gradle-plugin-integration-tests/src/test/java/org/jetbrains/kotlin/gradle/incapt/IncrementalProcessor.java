/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incapt;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;

/** Simple processor that generates a class for every annotated element (class, field, method). */
public class IncrementalProcessor extends AbstractProcessor {

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

                String packageName;
                if (element instanceof TypeElement) {
                    packageName = element.getEnclosingElement().getSimpleName().toString();
                }
                else {
                    packageName = element.getEnclosingElement().getEnclosingElement().getSimpleName().toString();
                }

                try (Writer writer = processingEnv.getFiler().createSourceFile(packageName + "." + name, element).openWriter()) {
                    writer.append("package ").append(packageName).append(";");
                    writer.append("\npublic class ").append(name).append(" {}");
                }
                catch (IOException ignored) {
                }
            }
        }

        return false;
    }
}