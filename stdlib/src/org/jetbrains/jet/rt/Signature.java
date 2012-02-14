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

package org.jetbrains.jet.rt;

import jet.TypeInfo;
import jet.typeinfo.TypeInfoProjection;

import java.util.*;

/**
* @author Stepan Koltsov
*/
class Signature {

    // TODO: make all these fields private

    final Class klazz;

    List<TypeInfoProjection> variables;
    Map<String,Integer> varNames;
    List<TypeInfo> superTypes;

    HashMap<Class,TypeInfo> superSignatures = new HashMap<Class,TypeInfo>();

    Signature(Class klazz) {
        this.klazz = klazz;
    }

    void afterParse() {
        List<TypeInfo> myVars = variables == null ? Collections.<TypeInfo>emptyList() : new LinkedList<TypeInfo>();
        if(variables != null)
            for(int i = 0; i != variables.size(); ++i)
                myVars.add(new TypeInfoVar(this, false, i));

        for(TypeInfo superType : superTypes) {
            if(superType instanceof TypeInfoImpl) {
                TypeInfoImpl type = (TypeInfoImpl) superType;
                Signature superSignature = TypeInfoParser.parse(type.signature.klazz);

                TypeInfo substituted = TypeInfoUtils.substitute(type, myVars);
                superSignatures.put(type.signature.klazz, substituted);

                List<TypeInfo> vars = Collections.emptyList();
                if(superType.getProjectionCount() != 0) {
                    vars = new LinkedList<TypeInfo>();
                    for(int i=0; i != superType.getProjectionCount(); ++i) {
                        TypeInfo substitute = TypeInfoUtils.substitute(superType.getProjection(i).getType(), myVars);
                        vars.add(substitute);
                    }
                }

                for(Map.Entry<Class,TypeInfo> entry : superSignature.superSignatures.entrySet()) {
                    superSignatures.put(entry.getKey(), TypeInfoUtils.substitute(entry.getValue(), vars));
                }
            }
        }
    }
}
