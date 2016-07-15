//
// Created by user on 7/11/16.
//

#include "kotlin_class_generator.h"
#include <iostream>
#include "kotlin_enum_generator.h"
#include "kotlin_field_generator.h"
#include <algorithm>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

class FieldGenerator;   // declared in "kotlin_file_generator.h"

void ClassGenerator::generateCode(io::Printer *printer, bool isBuilder) const {
    generateHeader(printer, isBuilder);
    printer->Indent();

    /**
    * Field generator should know if it is generating code for builder.
    * or for fair class to choose between 'val' and 'var'.
    * Also note that fields should be declared before init section.
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

    // builder, mergeFrom and only for fair classes
    if (!isBuilder) {
        printer->Print("\n");
        generateBuilder(printer);

        printer->Print("\n");
        generateMergeMethods(printer);
    }

    // build() and setters are only for builders
    if (isBuilder) {
        printer->Print("\n");
        generateBuildMethod(printer);

        printer->Print("\n");
        generateParseMethods(printer);
    }

    printer->Outdent();
    printer->Print("}\n");
}

ClassGenerator::ClassGenerator(Descriptor const *descriptor)
    : descriptor(descriptor) {
    simpleName = descriptor->name();
    builderName = "Builder" + simpleName;

    int field_count = descriptor->field_count();
    for (int i = 0; i < field_count; ++i) {
        FieldDescriptor const * fieldDescriptor = descriptor->field(i);
        properties.push_back(new FieldGenerator(fieldDescriptor, /* enclosingClass = */ this));
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

    /**
     * Sort properties in ascending order on their tag numbers. This order
     * affects order of serialization and deserialization, thus fields will be
     * serialized in order of their tags, as demanded by Google
     */
    std::sort(properties.begin(), properties.end(),
              [](FieldGenerator const * first, FieldGenerator const * second) {
                  return first->fieldNumber < second->fieldNumber;
              });
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

void ClassGenerator::generateMergeMethods(io::Printer *printer) const {
    map <string, string> vars;

    // mergeWith(other: Message)
    printer->Print("\n");
    vars["className"] = simpleName;
    printer->Print(vars, "fun mergeWith (other: $className$) {\n");
    printer->Indent();

    for (int i = 0; i < properties.size(); ++i) {
        vars["fieldName"] = properties[i]->simpleName;

        // concatenate repeated fields
        if (properties[i]->modifier == FieldDescriptor::LABEL_REPEATED) {
            printer->Print(vars, "$fieldName$.addAll(other.$fieldName$)\n");
        }

        // Bytes type is handled separately
        else if (properties[i]->protoType == FieldDescriptor::TYPE_BYTES) {
            vars["initValue"] = properties[i]->getInitValue();
            printer->Print(vars, "$fieldName$.plus(other.$fieldName$)\n");
        }

        // for all other cases just take other's field
        else {
            printer->Print(vars, "$fieldName$ = other.$fieldName$\n");
        }
    }

    printer->Outdent();
    printer->Print("}\n");


    // mergeFrom(input: CodedInputStream)
    printer->Print("\n");
    printer->Print(vars, "fun mergeFrom (input: CodedInputStream) {\n");
    printer->Indent();

    vars["builderName"] = builderName;
    printer->Print(vars, "val builder = $builderName$()\n");
    printer->Print("mergeWith(builder.parseFrom(input).build())");

    printer->Outdent();
    printer->Print("}\n");
}

void ClassGenerator::generateSerializers(io::Printer * printer, bool isRead) const {
    // readFrom(input: CodedInputStream) OR
    // writeTo(output: CodedOutputStream)
    map <string, string> vars;
    vars["funName"]= isRead ? "readFrom"            : "writeTo";
    vars["returnType"] = isRead ? builderName : "Unit";
    vars["stream"] = isRead ? "CodedInputStream"    : "CodedOutputStream";
    vars["arg"]    = isRead ? "input"               : "output";
    vars["maybeSeparator"] = isRead ? "" : ", ";
    vars["maybeReturn"] = isRead ? "return " : "";

    // generate function header
    printer->Print(vars,
                   "fun $funName$ ($arg$: $stream$): $returnType$ {"
                           "\n");
    printer->Indent();

    printer->Print(vars, "$maybeReturn$$funName$NoTag($arg$)\n");

    printer->Outdent();
    printer->Print("}\n");
}

void ClassGenerator::generateSerializersNoTag(io::Printer *printer, bool isRead) const {
    map <string, string> vars;
    vars["funName"]= isRead ? "readFromNoTag"       : "writeToNoTag";
    vars["stream"] = isRead ? "CodedInputStream"    : "CodedOutputStream";
    vars["arg"]    = isRead ? "input"               : "output";
    vars["returnType"] = isRead ? builderName : "Unit";
    vars["maybeReturn"] = isRead ? "return this\n" : "";

    // generate function header
    printer->Print(vars,
                   "fun $funName$ ($arg$: $stream$): $returnType$ {"
                   "\n");
    printer->Indent();

    // generate code for serialization/deserialization of fields
    for (int i = 0; i < properties.size(); ++i) {
        properties[i]->generateSerializationCode(printer, isRead);
    }

    printer->Print(vars, "$maybeReturn$");
    printer->Outdent();
    printer->Print("}\n");
}



void ClassGenerator::generateHeader(io::Printer * printer, bool isBuilder) const {
    // build list of arguments like 'field1: Type1, field2: Type2, ... '
    string argumentList = "";
    for (int i = 0; i < properties.size(); ++i) {
        argumentList += properties[i]->simpleName + ": " + properties[i]->fullType + " = " + properties[i]->getInitValue();
        if (i + 1 != properties.size()) {
            argumentList += ", ";
        }
    }

    map<string, string> vars;
    vars["name"] = isBuilder? builderName : simpleName;
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

void ClassGenerator::generateParseMethods(io::Printer *printer) const {
    // parseFieldFrom(input: CodedInputStream): Boolean
    map <string, string> vars;
    vars["builderName"] = builderName;

    printer->Print("fun parseFieldFrom(input: CodedInputStream): Boolean {\n");
    printer->Indent();

    // messages are not required to end with 0-tag, therefore parsing method should check for EOF
    printer->Print("if (input.isAtEnd()) { return false }\n");

    // read tag and check if some field will follow (0-tag inidcates end of message)
    printer->Print("val tag = input.readInt32NoTag()\n");
    printer->Print("if (tag == 0) { return false } \n");

    // parse tag into field number and wire type
    printer->Print("val fieldNumber = WireFormat.getTagFieldNumber(tag)\n");
    printer->Print("val wireType = WireFormat.getTagWireType(tag)\n");

    // 'when' to map fieldNumber into fieldName
    printer->Print("when(fieldNumber) {\n");
    printer->Indent();

    for (int i = 0; i < properties.size(); ++i) {
        vars["fieldNumber"] = std::to_string(properties[i]->fieldNumber);
        vars["kotlinFunSuffix"] = properties[i]->getKotlinFunctionSuffix();
        printer->Print(vars, "$fieldNumber$ -> ");

        // code for serialization arrays and messages consists of more than one line and needs enclosing brackets
        if (properties[i]->modifier == FieldDescriptor::LABEL_REPEATED
                || properties[i]->protoType == FieldDescriptor::TYPE_MESSAGE) {
            printer->Print("{\n");
            printer->Indent();
        }

        properties[i]->generateSerializationCode(printer, /* isRead = */ true, /* noTag = */ true);

        if (properties[i]->modifier == FieldDescriptor::LABEL_REPEATED
            || properties[i]->protoType == FieldDescriptor::TYPE_MESSAGE) {
            printer->Outdent();
            printer->Print("}\n");
        }
    }

    printer->Outdent();
    printer->Print("}\n");  // when-clause

    printer->Print("return true");
    printer->Outdent();
    printer->Print("}\n");  // parseFieldFrom body

    // parseFrom(input: CodedInputStream)
    printer->Print(vars,
                   "fun parseFrom(input: CodedInputStream): $builderName$ {\n");
    printer->Indent();
    printer->Print("while(parseFieldFrom(input)) {}\n");
    printer->Print("return this\n");

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


