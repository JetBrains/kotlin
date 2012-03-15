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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getSuperclassDescriptors;


//TODO: can optimise using less dumb implementation
//TODO: pass list of descriptors here, not the list of jet classes

/**
 * @author Pavel Talanov
 */
public final class ClassSortingUtils {

    private ClassSortingUtils() {
    }

    @NotNull
    public static List<JetClass> sortUsingInheritanceOrder(@NotNull List<JetClass> elements,
                                                           @NotNull BindingContext bindingContext) {
        List<ClassDescriptor> descriptors = descriptorsFromClasses(elements, bindingContext);
        PartiallyOrderedSet<ClassDescriptor> partiallyOrderedSet
                = new PartiallyOrderedSet<ClassDescriptor>(descriptors, inheritanceOrder());
        List<JetClass> sortedClasses = descriptorsToClasses(partiallyOrderedSet.partiallySortedElements(), bindingContext);
        assert elements.size() == sortedClasses.size();
        return sortedClasses;
    }

    @NotNull
    private static PartiallyOrderedSet.Order<ClassDescriptor> inheritanceOrder() {
        return new PartiallyOrderedSet.Order<ClassDescriptor>() {
            @Override
            public boolean firstDependsOnSecond(@NotNull ClassDescriptor first, @NotNull ClassDescriptor second) {
                return isDerivedClass(first, second);
            }
        };
    }

    private static boolean isDerivedClass(@NotNull ClassDescriptor ancestor, @NotNull ClassDescriptor derived) {
        return (getSuperclassDescriptors(derived).contains(ancestor));
    }

    @NotNull
    private static List<JetClass> descriptorsToClasses(@NotNull List<ClassDescriptor> descriptors,
                                                       @NotNull BindingContext bindingContext) {
        List<JetClass> sortedClasses = Lists.newArrayList();
        for (ClassDescriptor descriptor : descriptors) {
            sortedClasses.add(BindingUtils.getClassForDescriptor(bindingContext, descriptor));
        }
        return sortedClasses;
    }


    @NotNull
    private static List<ClassDescriptor> descriptorsFromClasses(@NotNull List<JetClass> classesToSort,
                                                                @NotNull BindingContext bindingContext) {
        List<ClassDescriptor> descriptorList = new ArrayList<ClassDescriptor>();
        for (JetClass jetClass : classesToSort) {
            descriptorList.add(BindingUtils.getClassDescriptor(bindingContext, jetClass));
        }
        return descriptorList;
    }

}
