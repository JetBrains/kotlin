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

package org.jetbrains.k2js.translate.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptorParent;

import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class NamespaceSortingUtils {

    private NamespaceSortingUtils() {
    }

    @NotNull
    public static List<NamespaceDescriptor> sortNamespacesUsingQualificationOrder
        (@NotNull List<NamespaceDescriptor> namespaceDescriptors) {
        PartiallyOrderedSet<NamespaceDescriptor> namespaceDescriptorPartiallyOrderedSet
            = new PartiallyOrderedSet<NamespaceDescriptor>(namespaceDescriptors, qualificationOrder());
        return namespaceDescriptorPartiallyOrderedSet.partiallySortedElements();
    }

    @NotNull
    private static PartiallyOrderedSet.Order<NamespaceDescriptor> qualificationOrder() {
        return new PartiallyOrderedSet.Order<NamespaceDescriptor>() {
            @Override
            public boolean firstDependsOnSecond(@NotNull NamespaceDescriptor first, @NotNull NamespaceDescriptor second) {
                NamespaceDescriptorParent containingDeclaration = first.getContainingDeclaration();
                if (containingDeclaration == null) {
                    return false;
                }
                return containingDeclaration.equals(second);
            }
        };
    }
}
