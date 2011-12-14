package org.jetbrains.jet.rt;

import jet.typeinfo.TypeInfo;
import jet.typeinfo.TypeInfoProjection;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
