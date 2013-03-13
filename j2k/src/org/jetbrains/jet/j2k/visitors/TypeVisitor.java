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

package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.J2KConverterFlags;
import org.jetbrains.jet.j2k.ast.*;
import org.jetbrains.jet.j2k.util.AstUtil;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.LinkedList;
import java.util.List;

public class TypeVisitor extends PsiTypeVisitor<Type> implements J2KVisitor {
    public static final String JAVA_LANG_BYTE = "java.lang.Byte";
    public static final String JAVA_LANG_CHARACTER = "java.lang.Character";
    public static final String JAVA_LANG_DOUBLE = "java.lang.Double";
    public static final String JAVA_LANG_FLOAT = "java.lang.Float";
    public static final String JAVA_LANG_INTEGER = "java.lang.Integer";
    public static final String JAVA_LANG_LONG = "java.lang.Long";
    public static final String JAVA_LANG_SHORT = "java.lang.Short";
    private static final String JAVA_LANG_BOOLEAN = "java.lang.Boolean";
    public static final String JAVA_LANG_OBJECT = "java.lang.Object";
    public static final String JAVA_LANG_STRING = "java.lang.String";
    private static final String JAVA_LANG_ITERABLE = "java.lang.Iterable";
    private static final String JAVA_UTIL_ITERATOR = "java.util.Iterator";
    private final Converter myConverter;
    private Type myResult = Type.EMPTY_TYPE;

    public TypeVisitor(@NotNull Converter myConverter) {
        this.myConverter = myConverter;
    }

    @NotNull
    public Type getResult() {
        return myResult;
    }

    @Override
    public Type visitPrimitiveType(@NotNull PsiPrimitiveType primitiveType) {
        String name = primitiveType.getCanonicalText();
        IdentifierImpl identifier = new IdentifierImpl(name);

        if (name.equals("void")) {
            myResult = new PrimitiveType(new IdentifierImpl(KotlinBuiltIns.UNIT_ALIAS.getName()));
        }
        else if (Node.PRIMITIVE_TYPES.contains(name)) {
            myResult = new PrimitiveType(new IdentifierImpl(AstUtil.upperFirstCharacter(name)));
        }
        else {
            myResult = new PrimitiveType(identifier);
        }
        return super.visitPrimitiveType(primitiveType);
    }

    @Override
    public Type visitArrayType(@NotNull PsiArrayType arrayType) {
        if (myResult == Type.EMPTY_TYPE) {
            myResult = new ArrayType(getConverter().typeToType(arrayType.getComponentType()));
        }
        return super.visitArrayType(arrayType);
    }

    @Override
    public Type visitClassType(@NotNull PsiClassType classType) {
        IdentifierImpl identifier = constructClassTypeIdentifier(classType);
        List<Type> resolvedClassTypeParams = createRawTypesForResolvedReference(classType);

        if (classType.getParameterCount() == 0 && resolvedClassTypeParams.size() > 0) {
            myResult = new ClassType(identifier, resolvedClassTypeParams);
        }
        else {
            myResult = new ClassType(identifier, getConverter().typesToTypeList(classType.getParameters()));
        }
        return super.visitClassType(classType);
    }

    @NotNull
    private IdentifierImpl constructClassTypeIdentifier(@NotNull PsiClassType classType) {
        PsiClass psiClass = classType.resolve();
        if (psiClass != null) {
            String qualifiedName = psiClass.getQualifiedName();
            if (qualifiedName != null) {
                if (!qualifiedName.equals("java.lang.Object") && getConverter().hasFlag(J2KConverterFlags.FULLY_QUALIFIED_TYPE_NAMES)) {
                    return new IdentifierImpl(qualifiedName);
                }
                if (qualifiedName.equals(JAVA_LANG_ITERABLE)) {
                    return new IdentifierImpl(JAVA_LANG_ITERABLE);
                }
                if (qualifiedName.equals(JAVA_UTIL_ITERATOR)) {
                    return new IdentifierImpl(JAVA_UTIL_ITERATOR);
                }
            }
        }
        String classTypeName = createQualifiedName(classType);

        if (classTypeName.isEmpty()) {
            return new IdentifierImpl(getClassTypeName(classType));
        }

        return new IdentifierImpl(classTypeName);
    }

    @NotNull
    private static String createQualifiedName(@NotNull PsiClassType classType) {
        String classTypeName = "";
        if (classType instanceof PsiClassReferenceType) {
            PsiJavaCodeReferenceElement reference = ((PsiClassReferenceType) classType).getReference();
            if (reference.isQualified()) {
                String result = new IdentifierImpl(reference.getReferenceName()).toKotlin();
                PsiElement qualifier = reference.getQualifier();
                while (qualifier != null) {
                    PsiJavaCodeReferenceElement p = (PsiJavaCodeReferenceElement) qualifier;
                    result = new IdentifierImpl(p.getReferenceName()).toKotlin() + "." + result; // TODO: maybe need to replace by safe call?
                    qualifier = p.getQualifier();
                }
                classTypeName = result;
            }
        }
        return classTypeName;
    }

    @NotNull
    private List<Type> createRawTypesForResolvedReference(@NotNull PsiClassType classType) {
        List<Type> typeParams = new LinkedList<Type>();
        if (classType instanceof PsiClassReferenceType) {
            PsiJavaCodeReferenceElement reference = ((PsiClassReferenceType) classType).getReference();
            PsiElement resolve = reference.resolve();
            if (resolve != null) {
                if (resolve instanceof PsiClass)
                //noinspection UnusedDeclaration
                {
                    for (PsiTypeParameter p : ((PsiClass) resolve).getTypeParameters()) {
                        Type boundType = p.getSuperTypes().length > 0 ?
                                         new ClassType(new IdentifierImpl(getClassTypeName(p.getSuperTypes()[0])), getConverter().typesToTypeList(p.getSuperTypes()[0].getParameters()), true)
                                                                      :
                                         new StarProjectionType();

                        typeParams.add(boundType);
                    }
                }
            }
        }
        return typeParams;
    }

    @NotNull
    private static String getClassTypeName(@NotNull PsiClassType classType) {
        String canonicalTypeStr = classType.getCanonicalText();
        if (canonicalTypeStr.equals(JAVA_LANG_OBJECT)) return "Any";
        if (canonicalTypeStr.equals(JAVA_LANG_BYTE)) return "Byte";
        if (canonicalTypeStr.equals(JAVA_LANG_CHARACTER)) return "Char";
        if (canonicalTypeStr.equals(JAVA_LANG_DOUBLE)) return "Double";
        if (canonicalTypeStr.equals(JAVA_LANG_FLOAT)) return "Float";
        if (canonicalTypeStr.equals(JAVA_LANG_INTEGER)) return "Int";
        if (canonicalTypeStr.equals(JAVA_LANG_LONG)) return "Long";
        if (canonicalTypeStr.equals(JAVA_LANG_SHORT)) return "Short";
        if (canonicalTypeStr.equals(JAVA_LANG_BOOLEAN)) return "Boolean";
        return classType.getClassName() != null ? classType.getClassName() : classType.getCanonicalText();
    }

    @Override
    public Type visitWildcardType(@NotNull PsiWildcardType wildcardType) {
        if (wildcardType.isExtends()) {
            myResult = new OutProjectionType(getConverter().typeToType(wildcardType.getExtendsBound()));
        }
        else if (wildcardType.isSuper()) {
            myResult = new InProjectionType(getConverter().typeToType(wildcardType.getSuperBound()));
        }
        else {
            myResult = new StarProjectionType();
        }
        return super.visitWildcardType(wildcardType);
    }

    @Override
    public Type visitEllipsisType(@NotNull PsiEllipsisType ellipsisType) {
        myResult = new VarArg(getConverter().typeToType(ellipsisType.getComponentType()));
        return super.visitEllipsisType(ellipsisType);
    }

    @Override
    public Type visitCapturedWildcardType(PsiCapturedWildcardType capturedWildcardType) {
        return super.visitCapturedWildcardType(capturedWildcardType);
    }

    @Override
    public Type visitDisjunctionType(PsiDisjunctionType disjunctionType) {
        return super.visitDisjunctionType(disjunctionType);
    }

    @NotNull
    @Override
    public Converter getConverter() {
        return myConverter;
    }
}

