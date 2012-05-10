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

/*
 * @author alex.tkachman
 */
package jet;

import org.jetbrains.jet.rt.annotation.AssertInvisibleInResolver;

@AssertInvisibleInResolver
public abstract class ExtensionFunction18<E, D1, D2, D3, D4, D5, D6, D7, D8, D9, D10, D11, D12, D13, D14, D15, D16, D17, D18, R> extends DefaultJetObject {
    protected ExtensionFunction18() {
        super();
    }

    public abstract R invoke(E receiver, D1 d1, D2 d2, D3 d3, D4 d4, D5 d5, D6 d6, D7 d7, D8 d8, D9 d9, D10 d10, D11 d11, D12 d12, D13 d13, D14 d14, D15 d15, D16 d16, D17 d17, D18 d18);

    @Override
    public String toString() {
      return "{E.(d1: D1, d2: D2, d3: D3, d4: D4, d5: D5, d6: D6, d7: D7, d8: D8, d9: D9, d10: D10, d11: D11, d12: D12, d13: D13, d14: D14, d15: D15, d16: D16, d17: D17, d18: D18) : R)}";
    }
}

