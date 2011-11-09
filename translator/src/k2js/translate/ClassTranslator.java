package k2js.translate;

import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Talanov Pavel
 */
//TODO ClassTranslator needs heavy improvement
public final class ClassTranslator extends AbstractTranslator {

    private final ClassBodyVisitor classBodyVisitor = new ClassBodyVisitor();

    public ClassTranslator(TranslationContext context) {
        super(context);
    }

    public JsStatement translateClass(JetClass classDeclaration) {
        JsInvocation jsClassDeclaration = new JsInvocation();
        jsClassDeclaration.setQualifier(Namer.creationMethodReference());
        JsObjectLiteral jsClassDescription = translateClassDeclarations(classDeclaration);
        jsClassDeclaration.getArguments().add(jsClassDescription);
        //getSuperClassName(classDeclaration);
        String className = classDeclaration.getName();
        JsName jsClassName = translationContext().namespaceScope().declareName(className);
        return AstUtil.convertToStatement(AstUtil.newAssignment(
                translationContext().getNamespaceQualifiedReference(jsClassName), jsClassDeclaration));
    }

    private void getSuperClassName(JetClass classDeclaration) {
        ClassDescriptor classDescriptor = bindingContext().get(BindingContext.CLASS, classDeclaration);
        JetType superType = classDescriptor.getSuperclassType();
        DeclarationDescriptor descriptor = superType.getConstructor().getDeclarationDescriptor();
        JetClass superClass = (JetClass)bindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        String superClassName = superClass.getName();
    }

    private JsObjectLiteral translateClassDeclarations(JetClass classDeclaration) {
        List<JsPropertyInitializer> propertyList = new ArrayList<JsPropertyInitializer>();
        propertyList.add(generateInitializeMethod(classDeclaration));
        propertyList.addAll(classDeclaration.accept(classBodyVisitor, translationContext()));
        return new JsObjectLiteral(propertyList);
    }

    private JsPropertyInitializer generateInitializeMethod(JetClass classDeclaration) {
        JsPropertyInitializer initializer = new JsPropertyInitializer();
        initializer.setLabelExpr(program().getStringLiteral(Namer.INITIALIZE_METHOD_NAME));
        initializer.setValueExpr(generateInitializeMethodBody(classDeclaration));
        return initializer;
    }

    private JsFunction generateInitializeMethodBody(JetClass classDeclaration) {
        return AstUtil.newFunction(scope(), null, null, new JsBlock());
    }



}
