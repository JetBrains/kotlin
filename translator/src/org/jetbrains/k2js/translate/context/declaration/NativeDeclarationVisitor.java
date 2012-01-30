package org.jetbrains.k2js.translate.context.declaration;

import org.jetbrains.annotations.NotNull;

import static org.jetbrains.k2js.translate.context.declaration.AnnotationsUtils.NATIVE_CLASS_ANNOTATION_FQNAME;
import static org.jetbrains.k2js.translate.context.declaration.AnnotationsUtils.NATIVE_FUNCTION_ANNOTATION_FQNAME;

/**
 * @author Pavel Talanov
 */
public final class NativeDeclarationVisitor extends AnnotatedDeclarationVisitor {

    /*package*/ NativeDeclarationVisitor(@NotNull Declarations nativeDeclarations) {
        super(nativeDeclarations, NATIVE_CLASS_ANNOTATION_FQNAME, NATIVE_FUNCTION_ANNOTATION_FQNAME);
    }
}
