/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.doc;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.compiler.CompilerPlugin;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * TODO This class is written in Java for now to work around a few gremlins in Kotlin...
 */
public abstract class KDocSupport implements CompilerPlugin {
    protected Set<NamespaceDescriptor> allNamespaces = new HashSet<NamespaceDescriptor>();
    protected Set<ClassDescriptor> allClasses = new HashSet<ClassDescriptor>();

    public void processFiles(BindingContext context, List<JetFile> sources) {
        for (JetFile source : sources) {
            // We retrieve a descriptor by a PSI element from the context
            NamespaceDescriptor namespaceDescriptor = context.get(BindingContext.NAMESPACE, source);
            if (namespaceDescriptor != null) {
                allNamespaces.add(namespaceDescriptor);
            }
        }

        for (NamespaceDescriptor namespace : allNamespaces) {
            // Let's take all the declarations in the namespace...
            processDescriptors(namespace.getMemberScope().getAllDescriptors(), context);
        }

        generate();
    }

    protected abstract void generate();

    private void processDescriptors(Collection<DeclarationDescriptor> allDescriptors, BindingContext context) {
        for (DeclarationDescriptor descriptor : allDescriptors) {
            PsiElement classElement = context.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
/*
            // Print the doc comment text
            String docComment = getDocCommentFor(classElement);
            if (docComment != null) {
                System.out.println("Docs for " + descriptor.getName() + ": " + docComment);
            }
            else {
                System.out.println("No docs for " + descriptor.getName());
            }
            // Print the class header (verbose)
            System.out.println(DescriptorRenderer.TEXT.render(descriptor));
*/
            // Process members, if any
            if (descriptor instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
                if (classElement != null) {
                    allClasses.add(classDescriptor);
                }
                //processDescriptors(classDescriptor.getDefaultType().getMemberScope().getAllDescriptors(), context);
            }
        }
    }
}
