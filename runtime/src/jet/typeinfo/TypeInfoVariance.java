/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package jet.typeinfo;

import org.jetbrains.jet.rt.signature.JetSignatureVariance;

public enum TypeInfoVariance {
    INVARIANT("", JetSignatureVariance.INVARIANT) ,
    IN("in", JetSignatureVariance.IN),
    OUT("out", JetSignatureVariance.OUT);

    private final String label;
    private final JetSignatureVariance variance;

    TypeInfoVariance(String label, JetSignatureVariance variance) {
        this.label = label;
        this.variance = variance;
    }

    @Override
    public String toString() {
        return label;
    }
}
