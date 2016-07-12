//
// Created by user on 7/11/16.
//

#include "kotlin_class_generator.h"
#include <iostream>
#include "kotlin_enum_generator.h"
#include "kotlin_field_generator.h"

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

void ClassGenerator::generateCode(io::Printer *printer, bool isBuilder) const {
    // print class header
    map<string, string> vars;
    vars["modifier"] = modifier.getName();
    vars["name"] = (isBuilder? "Builder" : "") + simpleName;
    printer->Print(vars,
                   "$modifier$ $name$ private constructor () {"
                           "\n"
    );
    printer->Indent();

    // generate code for nested classes declarations
    for (ClassGenerator *gen: classesDeclarations) {
        gen->generateCode(printer, isBuilder);
        printer->Print("\n\n"); // separate each definition from next code block with empty line
    }

    // generate code for nested enums declarations
    for (EnumGenerator *gen: enumsDeclaraions) {
        gen->generateCode(printer);
        printer->Print("\n\n"); // separate each definitions from next code block with empty line
    }

    // generate code for fields
    for (FieldGenerator *gen: properties) {
        gen->generateCode(printer);
        printer->Print("\n");
    }

    // generate constructor for builders
    if (isBuilder) {
        printer->Print("\n");
        generateConstructor(printer);
    }

    // generate builder for fair classes
    if (!isBuilder) {
        printer->Print("\n");
        generateBuilder(printer);
    }

    printer->Outdent();
    printer->Print("}");
}

ClassGenerator::ClassGenerator(Descriptor const *descriptor) {
    simpleName = descriptor->name();    // TODO: think about more careful class naming
    modifier   = ClassModifier(ClassModifier::CLASS);

    int field_count = descriptor->field_count();
    for (int i = 0; i < field_count; ++i) {
        FieldDescriptor const * fieldDescriptor = descriptor->field(i);
        properties.push_back(new FieldGenerator(fieldDescriptor));
    }

    int nested_types_count = descriptor->nested_type_count();
    for (int i = 0; i < nested_types_count; ++i) {
        Descriptor const * nestedClassDescriptor = descriptor->nested_type(i);
        classesDeclarations.push_back(new ClassGenerator(nestedClassDescriptor));
    }

    int enums_declarations_count = descriptor->enum_type_count();
    for (int i = 0; i < enums_declarations_count; ++i) {
        EnumDescriptor const * nestedEnumDescriptor = descriptor->enum_type(i);
        enumsDeclaraions.push_back(new EnumGenerator(nestedEnumDescriptor));
    }


}

ClassGenerator::~ClassGenerator() {
    for (int i = 0; i < properties.size(); ++i) {
        delete properties[i];
    }

    for (int i = 0; i < classesDeclarations.size(); ++i) {
        delete classesDeclarations[i];
    }

    for (int i = 0; i < enumsDeclaraions.size(); ++i) {
        delete enumsDeclaraions[i];
    }
}

void ClassGenerator::generateBuilder(io::Printer *) const {

}

void ClassGenerator::generateConstructor(io::Printer *printer, bool isBuilder) const {
    // generate header
    printer->Print("private constructor(\n");

    // place each argument of constructor in separate line for a prettier code
    // we indent twice to make arguments indentation larger than indentation of inner block
    printer->Indent();
    printer->Indent();
    for (int i = 0; i < properties.size(); ++i) {
        // generate argument definition
        map<string, string> vars;
        vars["name"] = properties[i] ->simpleName;
        vars["field"] = properties[i]->fieldName;
        printer->Print(vars,
                       "$name$: $field$");

        // if it's last property, then print closing bracket for argument list, otherwise put comma
        if (i + 1 == properties.size()) {
            printer->Print(") : this()\n");
            printer->Outdent();
            printer->Outdent();
            printer->Print("{");
            printer->Indent();
        }
        else {
            printer->Print(",");
        }

        printer->Print("\n");
    }

    // print body of constructor - just assign arguments to corresponding fields
    for (int i = 0; i < properties.size(); ++i) {
        map <string, string> vars;
        vars["name"] = properties[i]->simpleName;
        printer->Print(vars,
                        "this.$name$ = $name$"
                        "\n"
        );
    }
    printer->Outdent();
    printer->Print("}");
}


const string ClassModifier::getName() const {
    string result = "";
    switch (type) {
        case CLASS:
            result = "class";
            break;
        case INTERFACE:
            result = "interface";
            break;
    }
    return result;
}

ClassModifier::ClassModifier(ClassModifier::Type type)
    : type(type)
{ }

ClassModifier::ClassModifier() {
    type = CLASS;
}


} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google


