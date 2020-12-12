/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.incapt;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.*;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Simple processor that generates resource file that contains names of annotated elements.
 */
public class IncrementalAggregatingProcessor extends AbstractProcessor {

    private Set<String> values = new TreeSet<String>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("example.KotlinFilerGenerated");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                if (element instanceof TypeElement || element instanceof ExecutableElement || element instanceof VariableElement) {
                    values.add(element.getSimpleName().toString());
                }
            }
        }

        if (roundEnv.processingOver() && !values.isEmpty()) {
            try (Writer writer = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "generated.txt").openWriter()) {
                for (String value : values) {
                    writer.append(value).append("\n");
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            values.clear();
        }

        return true;
    }
}