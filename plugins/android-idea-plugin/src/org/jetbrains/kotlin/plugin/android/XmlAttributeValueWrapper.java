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

package org.jetbrains.kotlin.plugin.android;

import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.navigation.PsiElementNavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.meta.PsiMetaOwner;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Eugene.Kudelevsky
 * Date: Aug 6, 2009
 * Time: 7:08:02 PM
 * To change this template use File | Settings | File Templates.
 */

public class XmlAttributeValueWrapper implements XmlAttributeValue, PsiNamedElement, PsiElementNavigationItem {
    private final XmlAttributeValue myWrappee;
    private final String myFileName;
    private final String myDirName;

    public XmlAttributeValueWrapper(@NotNull XmlAttributeValue wrappeeElement) {
        if (!(wrappeeElement instanceof NavigationItem)) {
            throw new IllegalArgumentException();
        }
        if (!(wrappeeElement instanceof PsiMetaOwner)) {
            throw new IllegalArgumentException();
        }
        myWrappee = wrappeeElement;
        final PsiFile file = getContainingFile();
        myFileName = file != null ? file.getName() : null;
        final PsiDirectory dir = file != null ? file.getContainingDirectory() : null;
        myDirName = dir != null ? dir.getName() : null;
    }

    @Override
    @NotNull
    public Project getProject() throws PsiInvalidElementAccessException {
        return myWrappee.getProject();
    }

    @Override
    @NotNull
    public Language getLanguage() {
        return myWrappee.getLanguage();
    }

    @Override
    public PsiManager getManager() {
        return myWrappee.getManager();
    }

    @Override
    @NotNull
    public PsiElement[] getChildren() {
        return myWrappee.getChildren();
    }

    @Override
    public PsiElement getParent() {
        return myWrappee.getParent();
    }

    @Override
    @Nullable
    public PsiElement getFirstChild() {
        return myWrappee.getFirstChild();
    }

    @Override
    @Nullable
    public PsiElement getLastChild() {
        return myWrappee.getLastChild();
    }

    @Override
    @Nullable
    public PsiElement getNextSibling() {
        return myWrappee.getNextSibling();
    }

    @Override
    @Nullable
    public PsiElement getPrevSibling() {
        return myWrappee.getPrevSibling();
    }

    @Override
    public PsiFile getContainingFile() throws PsiInvalidElementAccessException {
        return myWrappee.getContainingFile();
    }

    @Override
    public TextRange getTextRange() {
        return myWrappee.getTextRange();
    }

    @Override
    public int getStartOffsetInParent() {
        return myWrappee.getStartOffsetInParent();
    }

    @Override
    public int getTextLength() {
        return myWrappee.getTextLength();
    }

    @Override
    @Nullable
    public PsiElement findElementAt(int offset) {
        return myWrappee.findElementAt(offset);
    }

    @Override
    @Nullable
    public PsiReference findReferenceAt(int offset) {
        return myWrappee.findReferenceAt(offset);
    }

    @Override
    public int getTextOffset() {
        return myWrappee.getTextOffset();
    }

    @Override
    @NonNls
    public String getText() {
        return myWrappee.getText();
    }

    @Override
    @NotNull
    public char[] textToCharArray() {
        return myWrappee.textToCharArray();
    }

    @Override
    public PsiElement getNavigationElement() {
        return myWrappee.getNavigationElement();
    }

    @Override
    public PsiElement getOriginalElement() {
        return myWrappee.getOriginalElement();
    }

    @Override
    public boolean textMatches(@NotNull @NonNls CharSequence text) {
        return myWrappee.textMatches(text);
    }

    @Override
    public boolean textMatches(@NotNull PsiElement element) {
        return myWrappee.textMatches(element);
    }

    @Override
    public boolean textContains(char c) {
        return myWrappee.textContains(c);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        myWrappee.accept(visitor);
    }

    @Override
    public void acceptChildren(@NotNull PsiElementVisitor visitor) {
        myWrappee.acceptChildren(visitor);
    }

    @NotNull
    @Override
    public PsiElement copy() {
        return myWrappee.copy();
    }

    @Override
    public PsiElement add(@NotNull PsiElement element) throws IncorrectOperationException {
        return myWrappee.add(element);
    }

    @Override
    public PsiElement addBefore(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        return myWrappee.addBefore(element, anchor);
    }

    @Override
    public PsiElement addAfter(@NotNull PsiElement element, PsiElement anchor) throws IncorrectOperationException {
        return myWrappee.addAfter(element, anchor);
    }

    @Override
    public void checkAdd(@NotNull PsiElement element) throws IncorrectOperationException {
        //noinspection deprecation
        myWrappee.checkAdd(element);
    }

    @Override
    public PsiElement addRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        return myWrappee.addRange(first, last);
    }

    @Override
    public PsiElement addRangeBefore(@NotNull PsiElement first, @NotNull PsiElement last, PsiElement anchor)
            throws IncorrectOperationException {
        return myWrappee.addRangeBefore(first, last, anchor);
    }

    @Override
    public PsiElement addRangeAfter(PsiElement first, PsiElement last, PsiElement anchor) throws IncorrectOperationException {
        return myWrappee.addRangeAfter(first, last, anchor);
    }

    @Override
    public void delete() throws IncorrectOperationException {
        myWrappee.delete();
    }

    @Override
    public void checkDelete() throws IncorrectOperationException {
        //noinspection deprecation
        myWrappee.checkDelete();
    }

    @Override
    public void deleteChildRange(PsiElement first, PsiElement last) throws IncorrectOperationException {
        myWrappee.deleteChildRange(first, last);
    }

    @NotNull
    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        return myWrappee.replace(newElement);
    }

    @Override
    public boolean isValid() {
        return myWrappee.isValid();
    }

    @Override
    public boolean isWritable() {
        return myWrappee.isWritable();
    }

    @Override
    @Nullable
    public PsiReference getReference() {
        return myWrappee.getReference();
    }

    @Override
    @NotNull
    public PsiReference[] getReferences() {
        return myWrappee.getReferences();
    }

    @Override
    @Nullable
    public <T> T getCopyableUserData(Key<T> key) {
        return myWrappee.getCopyableUserData(key);
    }

    @Override
    public <T> void putCopyableUserData(Key<T> key, T value) {
        myWrappee.putCopyableUserData(key, value);
    }

    @Override
    public boolean processDeclarations(
            @NotNull PsiScopeProcessor processor,
            @NotNull ResolveState state,
            @Nullable PsiElement lastParent,
            @NotNull PsiElement place
    ) {
        return myWrappee.processDeclarations(processor, state, lastParent, place);
    }

    @Override
    @Nullable
    public PsiElement getContext() {
        return myWrappee.getContext();
    }

    @Override
    public boolean isPhysical() {
        return myWrappee.isPhysical();
    }

    @Override
    @NotNull
    public GlobalSearchScope getResolveScope() {
        return myWrappee.getResolveScope();
    }

    @Override
    @NotNull
    public SearchScope getUseScope() {
        return myWrappee.getUseScope();
    }

    @Override
    @Nullable
    public ASTNode getNode() {
        return myWrappee.getNode();
    }

    @NonNls
    public String toString() {
        return myWrappee.toString();
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        return myWrappee == another || myWrappee.isEquivalentTo(another);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XmlAttributeValueWrapper that = (XmlAttributeValueWrapper) o;

        if (!myWrappee.equals(that.myWrappee)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return myWrappee.hashCode();
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        return myWrappee.getUserData(key);
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        myWrappee.putUserData(key, value);
    }

    @Override
    public Icon getIcon(int flags) {
        return myWrappee.getIcon(flags);
    }

    @Override
    public String getName() {
        return ((NavigationItem) myWrappee).getName();
    }

    @Override
    @Nullable
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        XmlAttribute attribute = (XmlAttribute) myWrappee.getParent();
        attribute.setValue(name);
        return null;
    }

    @Override
    public ItemPresentation getPresentation() {
        return new ItemPresentation() {
            @Override
            @Nullable
            public String getPresentableText() {
                String name = ((NavigationItem) myWrappee).getName();
                if (myDirName == null || myFileName == null) {
                    return name;
                }
                return name + " (..." + File.separatorChar + myDirName +
                       File.separatorChar + myFileName + ')';
            }

            @Override
            public String getLocationString() {
                return null;
            }

            @Override
            public Icon getIcon(boolean open) {
                return null;
            }
        };
    }

    @Override
    public void navigate(boolean requestFocus) {
        ((NavigationItem) myWrappee).navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return ((NavigationItem) myWrappee).canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return ((NavigationItem) myWrappee).canNavigateToSource();
    }

    @Override
    public String getValue() {
        return myWrappee.getValue();
    }

    @Override
    public TextRange getValueTextRange() {
        return myWrappee.getValueTextRange();
    }

    @Override
    public boolean processElements(PsiElementProcessor processor, PsiElement place) {
        return myWrappee.processElements(processor, place);
    }

    @Override
    public PsiElement getTargetElement() {
        return myWrappee;
    }
}

