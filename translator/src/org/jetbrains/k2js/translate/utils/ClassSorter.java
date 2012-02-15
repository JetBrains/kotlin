package org.jetbrains.k2js.translate.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getSuperclassDescriptors;


//TODO: can optimise using less dumb implementation

/**
 * @author Pavel Talanov
 */
public final class ClassSorter {

    @NotNull
    private final List<ClassDescriptor> descriptorList;
    @NotNull
    private final List<ClassDescriptor> classesWithNoAncestors;
    @NotNull
    private final Map<ClassDescriptor, Integer> classWasInheritedCount = new HashMap<ClassDescriptor, Integer>();
    @NotNull
    private final BindingContext bindingContext;

    @NotNull
    public static List<JetClass> sortUsingInheritanceOrder(@NotNull List<JetClass> original,
                                                           @NotNull BindingContext bindingContext) {
        ClassSorter sorter = new ClassSorter(original, bindingContext);
        return sorter.sortUsingInheritanceOrder();
    }

    private ClassSorter(@NotNull List<JetClass> original, @NotNull BindingContext bindingContext) {
        this.bindingContext = bindingContext;
        this.descriptorList = getDescriptorList(original);
        this.classesWithNoAncestors = new ArrayList<ClassDescriptor>(descriptorList);
        setInitialCount();
    }

    @NotNull
    private List<JetClass> sortUsingInheritanceOrder() {
        List<JetClass> sortedClasses = new ArrayList<JetClass>();
        while (!classesWithNoAncestors.isEmpty()) {
            ClassDescriptor classDescriptor = getNextClass();
            sortedClasses.add(BindingUtils.getClassForDescriptor(bindingContext, classDescriptor));
        }
        assert sortedClasses.size() == descriptorList.size();
        return sortedClasses;
    }

    @NotNull
    private ClassDescriptor getNextClass() {
        ClassDescriptor result = popFromList();
        decreaseCountForDerivedClasses(result);
        classWasInheritedCount.remove(result);
        return result;
    }

    private void decreaseCountForDerivedClasses(@NotNull ClassDescriptor result) {
        for (ClassDescriptor derived : descriptorList) {
            if (isDerivedClass(result, derived)) {
                decreaseCountForDerivedClass(derived);
            }
        }
    }

    private void decreaseCountForDerivedClass(@NotNull ClassDescriptor derived) {
        Integer timesInherited = classWasInheritedCount.get(derived);
        assert timesInherited != null;
        assert timesInherited > 0;
        int newCount = timesInherited - 1;
        classWasInheritedCount.put(derived, newCount);
        if (newCount == 0) {
            classesWithNoAncestors.add(derived);
        }
    }

    private boolean isDerivedClass(@NotNull ClassDescriptor ancestor, @NotNull ClassDescriptor derived) {
        return (getSuperclassDescriptors(derived).contains(ancestor));
    }

    @NotNull
    private ClassDescriptor popFromList() {
        assert !classesWithNoAncestors.isEmpty();
        ClassDescriptor result = classesWithNoAncestors.get(classesWithNoAncestors.size() - 1);
        ClassDescriptor removed = classesWithNoAncestors.remove(classesWithNoAncestors.size() - 1);
        assert removed != null;
        return result;
    }


    @NotNull
    private List<ClassDescriptor> getDescriptorList(@NotNull List<JetClass> classesToSort) {
        List<ClassDescriptor> descriptorList = new ArrayList<ClassDescriptor>();
        for (JetClass jetClass : classesToSort) {
            descriptorList.add(BindingUtils.getClassDescriptor(bindingContext, jetClass));
        }
        return descriptorList;
    }

    private void setInitialCount() {
        for (ClassDescriptor descriptor : descriptorList) {
            List<ClassDescriptor> superclasses = getSuperclassDescriptors(descriptor);
            int count = 0;
            for (ClassDescriptor superclassDescriptor : superclasses) {
                if (descriptorList.contains(superclassDescriptor)) {
                    count++;
                }
            }
            classWasInheritedCount.put(descriptor, superclasses.size());
            if (count > 0) {
                boolean success = classesWithNoAncestors.remove(descriptor);
                assert success;
            }
        }
    }

}
