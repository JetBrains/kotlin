package org.jetbrains.k2js.translate.declaration;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;

import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getAllNonNativeNamespaceDescriptors;

/**
 * @author Pavel Talanov
 */
public final class NamespaceDeclarationTranslator extends AbstractTranslator {

    public static List<JsStatement> translateFiles(@NotNull List<JetFile> files, @NotNull TranslationContext context) {
        List<JsStatement> result = Lists.newArrayList();
        for (NamespaceDescriptor namespaceDescriptor : getAllNonNativeNamespaceDescriptors(context.bindingContext(), files)) {
            result.add(Translation.translateNamespace(namespaceDescriptor, context));
        }
        return result;
    }

    private NamespaceDeclarationTranslator(@NotNull TranslationContext context) {
        super(context);
    }
}
