package org.jetbrains.jet.j2k.visitors;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.Converter;
import org.jetbrains.jet.j2k.ast.*;

import java.util.List;

import static org.jetbrains.jet.j2k.Converter.*;
import static org.jetbrains.jet.j2k.ConverterUtil.isAnnotatedAsNotNull;

/**
 * @author ignatov
 */
public class ElementVisitor extends JavaElementVisitor implements J2KVisitor {
    
    private final Converter myConverter;
    
    @NotNull
    private Element myResult = Element.EMPTY_ELEMENT;

    public ElementVisitor(@NotNull Converter converter) {
        this.myConverter = converter;
    }

    @Override
    @NotNull
    public Converter getConverter() {
        return myConverter;
    }

    @NotNull
    public Element getResult() {
        return myResult;
    }

    @Override
    public void visitLocalVariable(@NotNull PsiLocalVariable variable) {
        super.visitLocalVariable(variable);

        myResult = new LocalVariable(
                new IdentifierImpl(variable.getName()), // TODO
                modifiersListToModifiersSet(variable.getModifierList()),
                getConverter().typeToType(variable.getType(), isAnnotatedAsNotNull(variable.getModifierList())),
                getConverter().createSureCallOnlyForChain(variable.getInitializer(), variable.getType())
        );
    }

    @Override
    public void visitExpressionList(@NotNull PsiExpressionList list) {
        super.visitExpressionList(list);
        myResult = new ExpressionList(
                getConverter().expressionsToExpressionList(list.getExpressions()),
                getConverter().typesToTypeList(list.getExpressionTypes())
        );
    }

    @Override
    public void visitReferenceElement(@NotNull PsiJavaCodeReferenceElement reference) {
        super.visitReferenceElement(reference);

        final List<Type> types = getConverter().typesToTypeList(reference.getTypeParameters());
        if (!reference.isQualified()) {
            myResult = new ReferenceElement(
                    new IdentifierImpl(reference.getReferenceName()),
                    types
            );
        }
        else {
            String result = new IdentifierImpl(reference.getReferenceName()).toKotlin();
            PsiElement qualifier = reference.getQualifier();
            while (qualifier != null) {
                final PsiJavaCodeReferenceElement p = (PsiJavaCodeReferenceElement) qualifier;
                result = new IdentifierImpl(p.getReferenceName()).toKotlin() + "." + result; // TODO: maybe need to replace by safe call?
                qualifier = p.getQualifier();
            }
            myResult = new ReferenceElement(
                    new IdentifierImpl(result),
                    types
            );
        }
    }

    @Override
    public void visitTypeElement(@NotNull PsiTypeElement type) {
        super.visitTypeElement(type);
        myResult = new TypeElement(getConverter().typeToType(type.getType()));
    }

    @Override
    public void visitTypeParameter(@NotNull PsiTypeParameter classParameter) {
        super.visitTypeParameter(classParameter);
        myResult = new TypeParameter(
                new IdentifierImpl(classParameter.getName()), // TODO
                getConverter().typesToTypeList(classParameter.getExtendsListTypes())
        );
    }

    @Override
    public void visitParameterList(@NotNull PsiParameterList list) {
        super.visitParameterList(list);
        myResult = new ParameterList(
                getConverter().parametersToParameterList(list.getParameters())
        );
    }
}