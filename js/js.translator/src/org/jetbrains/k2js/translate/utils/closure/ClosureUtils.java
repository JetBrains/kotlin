/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.utils.closure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.resolve.BindingContext;

/**
 * @author Pavel Talanov
 */
public final class ClosureUtils {
    private ClosureUtils() {
    }

    @NotNull
    public static ClosureContext captureClosure(@NotNull BindingContext bindingContext, @NotNull JetElement element, @NotNull DeclarationDescriptor descriptor) {
        CaptureClosureVisitor captureClosureVisitor = new CaptureClosureVisitor(descriptor, bindingContext);
        ClosureContext closureContext = new ClosureContext();
        element.accept(captureClosureVisitor, closureContext);
        return closureContext;
    }
}
