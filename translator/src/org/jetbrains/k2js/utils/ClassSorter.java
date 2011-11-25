package org.jetbrains.k2js.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public final class ClassSorter {

    @NotNull
    private final List<JetClass> classesToSort;
    @NotNull
    private final List<ClassDescriptor> descriptorList;
    @NotNull
    private final BindingContext bindingContext;
    @NotNull
    private final PartiallyOrderedSet partiallyOrderedSet = new PartiallyOrderedSet();

    @NotNull
    static public List<JetClass> sortUsingInheritanceOrder(@NotNull List<JetClass> original,
                                                           @NotNull BindingContext bindingContext) {
        ClassSorter sorter = new ClassSorter(original, bindingContext);
        return sorter.sortUsingInheritanceOrder();
    }

    private ClassSorter(@NotNull List<JetClass> original, @NotNull BindingContext bindingContext) {
        this.classesToSort = original;
        this.bindingContext = bindingContext;
        this.descriptorList = getDescriptorList();
    }

    @NotNull
    private List<JetClass> sortUsingInheritanceOrder() {
        putDescriptorsInPartiallyOrderedSet();
        setInheritanceOrder();
        return getSortedClasses();
    }

    @NotNull
    private List<JetClass> getSortedClasses() {
        List<JetClass> sortedClasses = new ArrayList<JetClass>();
        for (Object object : partiallyOrderedSet) {
            assert object instanceof ClassDescriptor;
            sortedClasses.add(BindingUtils.getClassForDescriptor(bindingContext, (ClassDescriptor) object));
        }
        return sortedClasses;
    }

    private void putDescriptorsInPartiallyOrderedSet() {
        partiallyOrderedSet.addAll(descriptorList);
    }

    private void setInheritanceOrder() {
        for (ClassDescriptor descriptor : getDescriptorList()) {
            traverseAncestorsAndSetOrder(descriptor);
        }
    }

    @NotNull
    private List<ClassDescriptor> getDescriptorList() {
        List<ClassDescriptor> descriptorList = new ArrayList<ClassDescriptor>();
        for (JetClass jetClass : classesToSort) {
            descriptorList.add(BindingUtils.getClassDescriptor(bindingContext, jetClass));
        }
        return descriptorList;
    }

    private void traverseAncestorsAndSetOrder(@NotNull ClassDescriptor descriptor) {
        List<ClassDescriptor> superclasses = BindingUtils.getSuperclassDescriptors(descriptor);
        for (ClassDescriptor superclass : superclasses) {
            partiallyOrderedSet.setOrdering(superclass, descriptor);
            traverseAncestorsAndSetOrder(superclass);
        }
    }

}
