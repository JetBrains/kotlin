/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.reflect.KCallable;
import kotlin.reflect.KProperty;

public abstract class PropertyReference extends CallableReference implements KProperty {
    public PropertyReference() {
        super();
    }

    @SinceKotlin(version = "1.1")
    public PropertyReference(Object receiver) {
        super(receiver);
    }

    @Override
    @SinceKotlin(version = "1.1")
    protected KProperty getReflected() {
        return (KProperty) super.getReflected();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public boolean isLateinit() {
        return getReflected().isLateinit();
    }

    @Override
    @SinceKotlin(version = "1.1")
    public boolean isConst() {
        return getReflected().isConst();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof PropertyReference) {
            PropertyReference other = (PropertyReference) obj;
            return getOwner().equals(other.getOwner()) &&
                   getName().equals(other.getName()) &&
                   getSignature().equals(other.getSignature()) &&
                   Intrinsics.areEqual(getBoundReceiver(), other.getBoundReceiver());
        }
        if (obj instanceof KProperty) {
            return obj.equals(compute());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (getOwner().hashCode() * 31 + getName().hashCode()) * 31 + getSignature().hashCode();
    }

    @Override
    public String toString() {
        KCallable reflected = compute();
        if (reflected != this) {
            return reflected.toString();
        }

        return "property " + getName() + Reflection.REFLECTION_NOT_AVAILABLE;
    }
}
