//
// Created by user on 7/12/16.
//

#include "kotlin_enum_generator.h"
#include <iostream>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {


EnumValueGenerator::EnumValueGenerator(EnumValueDescriptor const *descriptor) {
    simpleName = descriptor->name();
    ordinal = descriptor->number();
}

void EnumValueGenerator::generateCode(io::Printer * printer) const {
    map <string, string> vars;
    vars["name"] = simpleName;
    vars["ordinal"] = std::to_string(ordinal);
    printer->Print(vars,
                   "$name$ ($ordinal$)"
    );
}

void EnumGenerator::generateCode(io::Printer * printer) const {
    /**
     *  Generate enum class header.
     *  Note that according to protobuf encoding, wire stores enum fields as ints,
     *  and client is responsible for proper casting those ints to actual enum values.
     *  Therefore, we have to add int-constructor in generated Kotlin-enum, and assign
     *  ordinals for each enum-value properly.
     */
    map <string, string> vars;
    vars["name"] = simpleName;
    printer->Print(vars,
                   "enum class $name$(val ord: Int) {"
                           "\n"
    );
    printer->Indent();

    // Generate enum values.
    for (int i = 0; i < enumValues.size(); ++i) {
        enumValues[i]->generateCode(printer);
        if (i + 1 != enumValues.size()) {
            printer->Print(",");
        }
        else {
            printer->Print(";");    // semicolon is necessary as companion object will follow
        }
        printer->Print("\n");
    }

    printer->Print("\n");
    gemerateEnumConverters(printer);

    printer->Outdent();
    printer->Print("}");
}

void EnumGenerator::gemerateEnumConverters(io::Printer *printer) const {
    map <string, string> vars;
    vars["dollar"] = "$";
    vars["type"] = simpleName;

    printer->Print("companion object {\n");
    printer->Indent();

    printer->Print(vars, "fun fromIntTo$type$ (ord: Int): $type$ {\n");
    printer->Indent();

    printer->Print("return when (ord) {\n");
    printer->Indent();

    // map ints to enum values
    for (int j = 0; j < enumValues.size(); ++j) {
        vars["ordinal"] = std::to_string(enumValues[j]->ordinal);
        vars["value"] = enumValues[j]->simpleName;

        printer->Print(vars,
                       "$ordinal$ -> $type$.$value$\n");
    }

    // catch cast errors in else-clause
    printer->Print(vars,
                   "else -> throw InvalidProtocolBufferException("
                           "\"Error: got unexpected int $dollar${ord} while parsing $type$ \""
                           ");\n");

    printer->Outdent();     // when-clause
    printer->Print("}\n");

    printer->Outdent();     // function body
    printer->Print("}\n");

    printer->Outdent();     // companion object body
    printer->Print("}\n");
}

EnumGenerator::~EnumGenerator() {
    for (int i = 0; i < enumValues.size(); ++i) {
        delete enumValues[i];
    }
}

EnumGenerator::EnumGenerator(EnumDescriptor const *descriptor) {
    simpleName = descriptor->name();
    int values_count = descriptor->value_count();
    for (int i = 0; i < values_count; ++i) {
        enumValues.push_back(new EnumValueGenerator(descriptor->value(i)));
    }
}

} // namespace kotlin
} // namspace compiler
} // namespace protobuf
} // namespace google
