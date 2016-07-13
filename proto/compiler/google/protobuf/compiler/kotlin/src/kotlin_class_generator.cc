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
    generateHeader(printer, isBuilder);
    printer->Indent();

    /**
    * Field generator should know if it is generating code for builder.
    * or for fair class to choose between 'val' and 'var'.
    */
    for (FieldGenerator *gen: properties) {
        gen->generateCode(printer, isBuilder);
        printer->Print("\n");
    }

    printer->Print("\n");
    generateInitSection(printer);

    // enum declarations and nested classes declarations only for fair classes
    if (!isBuilder) {
        for (EnumGenerator *gen: enumsDeclaraions) {
            gen->generateCode(printer);
            printer->Print("\n");
        }

        for (ClassGenerator *gen: classesDeclarations) {
            gen->generateCode(printer);
            printer->Print("\n");
        }
    }

    // write serialization methods only for fair classes, read methods only for Builders)
    printer->Print("\n");
    generateSerializers(printer, /* isRead = */ isBuilder);
    printer->Print("\n");
    generateSerializersNoTag(printer, /* isRead = */ isBuilder);

    // builder and mergeFrom only for fair classes
    if (!isBuilder) {
        printer->Print("\n");
        generateBuilder(printer);

        printer->Print("\n");
        generateMergeFrom(printer);
    }

    // build() is only for builders
    if (isBuilder) {
        printer->Print("\n");
        generateBuildMethod(printer);
    }

    printer->Outdent();
    printer->Print("}\n");
}

ClassGenerator::ClassGenerator(Descriptor const *descriptor)
    : descriptor(descriptor) {
    simpleName = descriptor->name();    // TODO: think about more careful class naming

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

void ClassGenerator::generateBuilder(io::Printer * printer) const {
    //XXX: just reuse generateCode with flag isBuilder set
    generateCode(printer, /* isBuilder = */ true);
}

void ClassGenerator::generateMergeFrom(io::Printer * printer) const {
    //TODO: Looks pretty dirty. Should reconsider process of generating readFrom, mergeFrom and writeTo.
    map <string, string> vars;
    printer->Print(vars, "fun mergeFrom (input: CodedInputStream) {\n");
    printer->Indent();

    for (int i = 0; i < properties.size(); ++i) {
        properties[i]->generateSerializationCode(printer, /* isRead = */ true);
    }

    printer->Outdent();
    printer->Print("}\n");
}

void ClassGenerator::generateSerializers(io::Printer * printer, bool isRead) const {
    map <string, string> vars;
    vars["funName"]= isRead ? "readFrom"            : "writeTo";
    vars["stream"] = isRead ? "CodedInputStream"    : "CodedOutputStream";
    vars["arg"]    = isRead ? "input"               : "output";
    vars["maybeSeparator"] = isRead ? "" : ", ";

    // generate function header
    printer->Print(vars,
                   "fun $funName$ ($arg$: $stream$) {"
                           "\n");
    printer->Indent();

    //TODO: write message tag and size
    printer->Print(vars, "$funName$NoTag($arg$)\n");

    printer->Outdent();
    printer->Print("}\n");
}

void ClassGenerator::generateSerializersNoTag(io::Printer *printer, bool isRead) const {
    map <string, string> vars;
    vars["funName"]= isRead ? "readFromNoTag"       : "writeToNoTag";
    vars["stream"] = isRead ? "CodedInputStream"    : "CodedOutputStream";
    vars["arg"]    = isRead ? "input"               : "output";

    // generate function header
    printer->Print(vars,
                   "fun $funName$ ($arg$: $stream$) {"
                   "\n");
    printer->Indent();

    // generate code for serialization/deserialization of fields
    for (int i = 0; i < properties.size(); ++i) {
        properties[i]->generateSerializationCode(printer, isRead);
    }

    printer->Outdent();
    printer->Print("}\n");
}



void ClassGenerator::generateHeader(io::Printer * printer, bool isBuilder) const {
    // build list of arguments like 'field1: Type1, field2: Type2, ... '
    string argumentList = "";
    for (int i = 0; i < properties.size(); ++i) {
        argumentList += properties[i]->simpleName + ": " + properties[i]->fieldName;
        if (i + 1 != properties.size()) {
            argumentList += ", ";
        }
    }

    map<string, string> vars;
    vars["name"] = (isBuilder? "Builder" : "") + simpleName;
    vars["argumentList"] = argumentList;
    vars["maybePrivate"] = isBuilder? "" : " private";
    printer->Print(vars,
                   "class $name$$maybePrivate$ constructor ($argumentList$) {"
                           "\n"
    );
}

void ClassGenerator::generateBuildMethod(io::Printer * printer) const {
    map <string, string> vars;
    vars["returnType"] = simpleName;
    printer->Print(vars,
                    "fun build(): $returnType$ {\n");
    printer->Indent();

    // pass all fields to constructor of enclosing class
    printer->Print(vars,
                    "return $returnType$(");
    for (int i = 0; i < properties.size(); ++i) {
        printer->Print(properties[i]->simpleName.c_str());
        if (i + 1 != properties.size()) {
            printer->Print(", ");
        }
    }
    printer->Print(")\n");
    printer->Outdent();
    printer->Print("}\n");
}

void ClassGenerator::generateInitSection(io::Printer * printer) const {
    printer->Print("init {\n");
    printer->Indent();

    for (int i = 0; i < properties.size(); ++i) {
        map <string, string> vars;
        vars["name"] = properties[i]->simpleName;
        printer->Print(vars,
                       "this.$name$ = $name$"
                               "\n"
        );
    }

    printer->Outdent();
    printer->Print("}\n");
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


