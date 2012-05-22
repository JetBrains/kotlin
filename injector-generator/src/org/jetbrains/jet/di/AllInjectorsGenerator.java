/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.di;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.codegen.ClassBuilderFactory;
import org.jetbrains.jet.codegen.ClassBuilderMode;
import org.jetbrains.jet.codegen.ClassCodegen;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.codegen.GenerationState;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.ScriptCodegen;
import org.jetbrains.jet.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.io.IOException;

/**
 * @author abreslav
 */
// NOTE: After making changes, you need to re-generate the injectors.
//       To do that, you can run either this class, or /build.xml/generateInjectors task
public class AllInjectorsGenerator {

    public static void main(String[] args) throws IOException {
        generateInjectorForTopDownAnalyzerBasic();
        generateInjectorForTopDownAnalyzerForJvm();
        generateInjectorForTopDownAnalyzerForJs();
        generateMacroInjector();
        generateTestInjector();
        generateInjectorForJavaSemanticServices();
        generateInjectorForJvmCodegen();
        generateInjectorForJetTypeMapper();
    }

    private static void generateInjectorForTopDownAnalyzerBasic() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
        generateInjectorForTopDownAnalyzerCommon(generator);
        generator.addParameter(ModuleConfiguration.class);
        generator.generate("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerBasic");
    }

    private static void generateInjectorForTopDownAnalyzerForJs() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
        generateInjectorForTopDownAnalyzerCommon(generator);
        generator.addParameter(ModuleConfiguration.class);
        generator.generate("js/js.translator/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerForJs");
    }

    private static void generateInjectorForTopDownAnalyzerForJvm() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
        generateInjectorForTopDownAnalyzerCommon(generator);
        generator.addParameter(CompilerDependencies.class);
        generator.addField(false, CompilerSpecialMode.class, null, new GivenExpression("compilerDependencies.getCompilerSpecialMode()"));
        generator.addPublicField(JavaBridgeConfiguration.class);
        generator.addField(PsiClassFinderForJvm.class);
        generator.generate("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerForJvm");
    }

    private static void generateInjectorForTopDownAnalyzerCommon(DependencyInjectorGenerator generator) {
        // Fields
        generator.addPublicField(TopDownAnalyzer.class);
        generator.addPublicField(TopDownAnalysisContext.class);
        generator.addPublicField(BodyResolver.class);
        generator.addPublicField(ControlFlowAnalyzer.class);
        generator.addPublicField(DeclarationsChecker.class);
        generator.addPublicField(DescriptorResolver.class);

        // Parameters
        generator.addPublicParameter(Project.class);
        generator.addPublicParameter(TopDownAnalysisParameters.class);
        generator.addPublicParameter(ObservableBindingTrace.class);
        generator.addParameter(ModuleDescriptor.class);
        generator.addParameter(JetControlFlowDataTraceFactory.class, false);
    }

    private static void generateMacroInjector() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);

        // Fields
        generator.addPublicField(ExpressionTypingServices.class);

        // Parameters
        generator.addPublicParameter(Project.class);

        generator.generate("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForMacros");
    }

    private static void generateTestInjector() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);

        // Fields
        generator.addPublicField(DescriptorResolver.class);
        generator.addPublicField(ExpressionTypingServices.class);
        generator.addPublicField(TypeResolver.class);
        generator.addPublicField(CallResolver.class);
        generator.addField(true, JetStandardLibrary.class, null, new GivenExpression("JetStandardLibrary.getInstance()"));

        // Parameters
        generator.addPublicParameter(Project.class);

        generator.generate("compiler/tests", "org.jetbrains.jet.di", "InjectorForTests");
    }
    
    private static void generateInjectorForJavaSemanticServices() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
        
        // Fields
        generator.addPublicField(JavaSemanticServices.class);
        generator.addPublicField(JavaDescriptorResolver.class);
        generator.addField(true, BindingTrace.class, null,
                new GivenExpression("new org.jetbrains.jet.lang.resolve.BindingTraceContext()"));
        generator.addField(JavaBridgeConfiguration.class);
        generator.addPublicField(PsiClassFinderForJvm.class);
        generator.addField(false, ModuleDescriptor.class, null,
                new GivenExpression("new org.jetbrains.jet.lang.descriptors.ModuleDescriptor(\"<dummy>\")"));
        generator.addParameter(CompilerDependencies.class);
        generator.addField(false, CompilerSpecialMode.class, null, new GivenExpression("compilerDependencies.getCompilerSpecialMode()"));

        // Parameters
        generator.addPublicParameter(Project.class);
        
        generator.generate("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForJavaSemanticServices");
    }

    private static void generateInjectorForJvmCodegen() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
        generator.addPublicParameter(JetStandardLibrary.class);
        generator.addParameter(BindingContext.class);
        generator.addParameter(DiType.listOf(JetFile.class));
        generator.addParameter(Project.class);
        generator.addParameter(CompilerSpecialMode.class);
        generator.addParameter(ClassBuilderMode.class);
        generator.addPublicParameter(GenerationState.class);
        generator.addParameter(ClassBuilderFactory.class);
        generator.addPublicField(JetTypeMapper.class);
        generator.addPublicField(ClassCodegen.class);
        generator.addPublicField(ScriptCodegen.class);
        generator.addField(true, IntrinsicMethods.class, "intrinsics", null);
        generator.addPublicField(ClassFileFactory.class);
        generator.generate("compiler/backend/src", "org.jetbrains.jet.di", "InjectorForJvmCodegen");
    }

    private static void generateInjectorForJetTypeMapper() throws IOException {
        DependencyInjectorGenerator generator = new DependencyInjectorGenerator(false);
        generator.addParameter(JetStandardLibrary.class);
        generator.addParameter(BindingContext.class);
        generator.addParameter(DiType.listOf(JetFile.class));
        generator.addPublicField(JetTypeMapper.class);
        generator.addField(CompilerSpecialMode.REGULAR);
        generator.addField(ClassBuilderMode.FULL);
        generator.generate("compiler/backend/src", "org.jetbrains.jet.di", "InjectorForJetTypeMapper");
    }

}
