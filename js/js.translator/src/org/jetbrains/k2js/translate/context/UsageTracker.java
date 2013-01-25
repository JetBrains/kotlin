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
package org.jetbrains.k2js.translate.context;

import com.intellij.util.Consumer;
import com.intellij.util.SmartList;
import com.intellij.util.containers.OrderedSet;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils;

import java.util.List;
import java.util.Set;

public final class UsageTracker {
    @Nullable
    private final ClassDescriptor trackedClassDescriptor;
    @NotNull
    private final MemberDescriptor memberDescriptor;

    @Nullable
    private final UsageTracker parent;
    @Nullable
    private List<UsageTracker> children;

    private boolean used;
    @Nullable
    private Set<CallableDescriptor> capturedVariables;
    private ClassDescriptor outerClassDescriptor;

    public UsageTracker(@NotNull MemberDescriptor memberDescriptor, @Nullable UsageTracker parent, @Nullable ClassDescriptor trackedClassDescriptor) {
        this.memberDescriptor = memberDescriptor;
        this.trackedClassDescriptor = trackedClassDescriptor;
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }

    public boolean isUsed() {
        return used;
    }

    private void addChild(UsageTracker child) {
        if (children == null) {
            children = new SmartList<UsageTracker>();
        }
        children.add(child);
    }

    private void addCapturedMember(CallableDescriptor descriptor) {
        if (capturedVariables == null) {
            capturedVariables = new OrderedSet<CallableDescriptor>();
        }
        capturedVariables.add(descriptor);
    }

    public void triggerUsed(DeclarationDescriptor descriptor) {
        if ((descriptor instanceof PropertyDescriptor || descriptor instanceof PropertyAccessorDescriptor)) {
            checkOuterClass(descriptor);
        }
        else if (descriptor instanceof VariableDescriptor) {
            VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
            if ((capturedVariables == null || !capturedVariables.contains(variableDescriptor)) &&
                !isAncestor(memberDescriptor, variableDescriptor)) {
                addCapturedMember(variableDescriptor);
            }
        }
        else if (descriptor instanceof SimpleFunctionDescriptor) {
            checkOuterClass(descriptor);
            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            // local named function
            CallableDescriptor callableDescriptor = (CallableDescriptor) descriptor;
            if (!JsDescriptorUtils.isExtension(callableDescriptor) &&
                !(containingDeclaration instanceof ClassOrNamespaceDescriptor) &&
                !isAncestor(memberDescriptor, descriptor)) {
                addCapturedMember(callableDescriptor);
            }
        }
        else if (descriptor instanceof ClassDescriptor) {
            if (trackedClassDescriptor == descriptor) {
                used = true;
            }
            else if (parent != null) {
                UsageTracker p = parent;
                do {
                    if (p.trackedClassDescriptor == descriptor) {
                        break;
                    }
                }
                while ((p = p.parent) != null);
            }
        }
    }

    private void checkOuterClass(DeclarationDescriptor descriptor) {
        if (outerClassDescriptor == null) {
            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            if (containingDeclaration instanceof ClassDescriptor) {
                outerClassDescriptor = (ClassDescriptor) containingDeclaration;
            }
        }
    }

    @Nullable
    public ClassDescriptor getOuterClassDescriptor() {
        if (outerClassDescriptor == null && parent != null) {
            UsageTracker p = parent;
            do {
                if (p.outerClassDescriptor != null) {
                    return p.outerClassDescriptor;
                }
            }
            while ((p = p.parent) != null);
        }
        return outerClassDescriptor;
    }

    public void forEachCaptured(Consumer<CallableDescriptor> consumer) {
        forEachCaptured(consumer, this, children == null ? null : new THashSet<CallableDescriptor>());
    }

    private boolean isOneOfTheMyParentsIsAncestor(VariableDescriptor parameterDescriptor, UsageTracker requestor) {
        if (requestor == this) {
            return false;
        }

        FunctionDescriptor paramOwner = (FunctionDescriptor) parameterDescriptor.getContainingDeclaration();
        UsageTracker p = parent;
        do {
            if (p.memberDescriptor == paramOwner ||
                p.memberDescriptor instanceof ClassDescriptor && p.memberDescriptor == paramOwner.getContainingDeclaration()) {
                return true;
            }
        }
        while ((p = p.parent) != null && p != requestor);
        return false;
    }

    private void forEachCaptured(Consumer<CallableDescriptor> consumer, UsageTracker requestor, @Nullable THashSet<CallableDescriptor> visited) {
        if (capturedVariables != null) {
            for (CallableDescriptor variable : capturedVariables) {
                if (variable instanceof VariableDescriptor && isOneOfTheMyParentsIsAncestor((VariableDescriptor) variable, requestor)) {
                    continue;
                }

                if (visited == null || visited.add(variable)) {
                    consumer.consume(variable);
                }
            }
        }
        if (children != null) {
            for (UsageTracker child : children) {
                child.forEachCaptured(consumer, requestor, visited);
            }
        }
    }

    public boolean hasCaptured() {
        if (capturedVariables != null) {
            assert !capturedVariables.isEmpty();
            return true;
        }

        if (children != null) {
            for (UsageTracker child : children) {
                if (child.hasCaptured()) {
                    return true;
                }
            }
        }
        return false;
    }

    // differs from DescriptorUtils - fails if reach NamespaceDescriptor
    private static boolean isAncestor(
            @NotNull DeclarationDescriptor ancestor,
            @NotNull DeclarationDescriptor declarationDescriptor
    ) {
        DeclarationDescriptor descriptor = declarationDescriptor.getContainingDeclaration();
        while (descriptor != null && !(descriptor instanceof NamespaceDescriptor)) {
            if (ancestor == descriptor) {
                return true;
            }
            descriptor = descriptor.getContainingDeclaration();
        }
        return false;
    }
}
