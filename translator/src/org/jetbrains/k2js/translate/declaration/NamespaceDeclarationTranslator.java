package org.jetbrains.k2js.translate.declaration;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import java.util.List;
import java.util.Set;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getAllNonNativeNamespaceDescriptors;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getAllClassesDefinedInNamespace;

/**
 * @author Pavel Talanov
 */
public final class NamespaceDeclarationTranslator extends AbstractTranslator {

    public static List<JsStatement> translateFiles(@NotNull List<JetFile> files, @NotNull TranslationContext context) {
        Set<NamespaceDescriptor> namespaceDescriptorSet = getAllNonNativeNamespaceDescriptors(context.bindingContext(), files);
        return (new NamespaceDeclarationTranslator(Lists.newArrayList(namespaceDescriptorSet), context)).translate();
    }

    @NotNull
    private final ClassDeclarationTranslator classDeclarationTranslator;
    @NotNull
    private final List<NamespaceDescriptor> namespaceDescriptors;

    private NamespaceDeclarationTranslator(@NotNull List<NamespaceDescriptor> namespaceDescriptors,
                                           @NotNull TranslationContext context) {
        super(context);
        this.namespaceDescriptors = namespaceDescriptors;
        this.classDeclarationTranslator = new ClassDeclarationTranslator(getAllClasses(), context);
    }

    @NotNull
    private List<ClassDescriptor> getAllClasses() {
        List<ClassDescriptor> result = Lists.newArrayList();
        for (NamespaceDescriptor namespaceDescriptor : namespaceDescriptors) {
            result.addAll(getAllClassesDefinedInNamespace(namespaceDescriptor));
        }
        return result;
    }

    //TODO: logic seems not so clear, may be a couple of extract method should help
    @NotNull
    private List<JsStatement> translate() {
        classDeclarationTranslator.generateDeclarations();
        List<JsStatement> result = Lists.newArrayList();
        result.add(classDeclarationsStatement());
        List<NamespaceTranslator> namespaceTranslators = getTranslatorsForNonEmptyNamespaces();
        result.addAll(declarationStatements(namespaceTranslators));
        result.addAll(initializeStatements(namespaceTranslators));
        return result;
    }

    @NotNull
    private JsStatement classDeclarationsStatement() {
        return classDeclarationTranslator.getDeclarationsStatement();
    }

    @NotNull
    private List<NamespaceTranslator> getTranslatorsForNonEmptyNamespaces() {
        List<NamespaceTranslator> namespaceTranslators = Lists.newArrayList();
        for (NamespaceDescriptor descriptor : namespaceDescriptors) {
            NamespaceTranslator namespaceTranslator =
                    new NamespaceTranslator(descriptor, classDeclarationTranslator, context());
            if (!namespaceTranslator.isNamespaceEmpty()) {
                namespaceTranslators.add(namespaceTranslator);
            }
        }
        return namespaceTranslators;
    }

    @NotNull
    private List<JsStatement> declarationStatements(@NotNull List<NamespaceTranslator> namespaceTranslators) {
        List<JsStatement> result = Lists.newArrayList();
        for (NamespaceTranslator translator : namespaceTranslators) {
            result.add(translator.getDeclarationStatement());
        }
        return result;
    }

    @NotNull
    private List<JsStatement> initializeStatements(@NotNull List<NamespaceTranslator> namespaceTranslators) {
        List<JsStatement> result = Lists.newArrayList();
        for (NamespaceTranslator translator : namespaceTranslators) {
            result.add(translator.getInitializeStatement());
        }
        return result;
    }
}
