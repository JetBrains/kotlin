//
// Created by user on 7/12/16.
//

#include "kotlin_field_generator.h"
#include <vector>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/descriptor.h>
#include "kotlin_name_resolver.h"
#include <iostream>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

/**
 * Gets equivalent Kotlin-type for a given field descriptor.
 * This method takes protobuf field type into account, adding 'List<>'
 * for repeated fields, and '?' for optional.
 */
string FieldGenerator::protobufToKotlinField() const {
    string  preamble = "",
            postamble = "";
    switch (modifier) {
        case FieldDescriptor::LABEL_REQUIRED:
            break;
        case FieldDescriptor::LABEL_OPTIONAL:
            postamble = "?";
            break;
        case FieldDescriptor::LABEL_REPEATED:
        #ifndef KOTLIN_GENERATED_CODE_LANGUAGE_LEVEL_LOW
            preamble = "MutableList <";
            postamble = "> ";
            break;
        #else
            preamble  = "Array <";
            postamble = "> ";
            break;
        #endif
    }
    return preamble + protobufToKotlinType() + postamble;
}

/**
 * Simply maps protobuf field type to corresponding Kotlin.
 */
string FieldGenerator::protobufToKotlinType() const {
    FieldDescriptor::Type type = descriptor->type();
    switch(type) {
        case FieldDescriptor::TYPE_BOOL:
            return "Boolean";
        case FieldDescriptor::TYPE_BYTES:
            return "ByteArray";
        case FieldDescriptor::TYPE_DOUBLE:
            return "Double";
        case FieldDescriptor::TYPE_ENUM:
            return string(descriptor->enum_type()->name());
        case FieldDescriptor::TYPE_FIXED32:
            // we map uint32 into Int, storing top bit in sign bit
            return "Int";
        case FieldDescriptor::TYPE_FIXED64:
            // we map uint64 into Long, storing top bit in sign bit
            return "Long";
        case FieldDescriptor::TYPE_FLOAT:
            return "Float";
        case FieldDescriptor::TYPE_INT32:
            return "Int";
        case FieldDescriptor::TYPE_INT64:
            return "Long";
        case FieldDescriptor::TYPE_MESSAGE:
            return string(descriptor->message_type()->name());
        case FieldDescriptor::TYPE_SFIXED32:
            return "Int";
        case FieldDescriptor::TYPE_SFIXED64:
            return "Long";
        case FieldDescriptor::TYPE_SINT32:
            return "Int";
        case FieldDescriptor::TYPE_SINT64:
            return "Long";
        case FieldDescriptor::TYPE_STRING:
            return "kotlin.String";
        case FieldDescriptor::TYPE_UINT32:
            return "Int";            // see notes for TYPE_FIXED32
        case FieldDescriptor::TYPE_UINT64:
            return "Long";           // see notes for TYPE_FIXED64
    }
}

string FieldGenerator::getInitValue() const {
    if (descriptor->is_repeated())
    #ifndef KOTLIN_GENERATED_CODE_LANGUAGE_LEVEL_LOW
        return "mutableListOf()";
    #else
        return "arrayOf()";
    #endif

    FieldDescriptor::Type type = descriptor->type();
    switch(type) {
        case FieldDescriptor::TYPE_BOOL:
            return "false";
        case FieldDescriptor::TYPE_BYTES:
            return "ByteArray(0)";
        case FieldDescriptor::TYPE_DOUBLE:
            return "0.0";
        case FieldDescriptor::TYPE_ENUM: {
            string enumType = descriptor->enum_type()->name();
            return enumType + ".fromIntTo" + enumType + "(0)";   // produce enum from 0, as demanded by Google
        }
        case FieldDescriptor::TYPE_FIXED32:
            return "0";
        case FieldDescriptor::TYPE_FIXED64:
            return "0L";
        case FieldDescriptor::TYPE_FLOAT:
            return "0f";
        case FieldDescriptor::TYPE_INT32:
            return "0";
        case FieldDescriptor::TYPE_INT64:
            return "0L";
        case FieldDescriptor::TYPE_MESSAGE:
            return string(descriptor->message_type()->name()) + "()";
        case FieldDescriptor::TYPE_SFIXED32:
            return "0";
        case FieldDescriptor::TYPE_SFIXED64:
            return "0L";
        case FieldDescriptor::TYPE_SINT32:
            return "0";
        case FieldDescriptor::TYPE_SINT64:
            return "0L";
        case FieldDescriptor::TYPE_STRING:
            return "\"\"";
        case FieldDescriptor::TYPE_UINT32:
            return "0";            // see notes for TYPE_FIXED32
        case FieldDescriptor::TYPE_UINT64:
            return "0L";           // see notes for TYPE_FIXED64
    }
}

void FieldGenerator::generateCode(io::Printer *printer, bool isBuilder, string className) const {
    map<string, string> vars;
    vars["name"] = simpleName;
    vars["field"] = protobufToKotlinField();
    printer->Print(vars, "var $name$ : $field$\n");

    // make setter private
    printer->Indent();
    printer->Print("private set\n");
    printer->Outdent();

    // generate setter for builder
    if (isBuilder) {
        generateSetter(printer, /* builderName = */ "Builder" + className);
    }

    // generate additional methods for repeated fields
    if (modifier == FieldDescriptor::LABEL_REPEATED) {
        generateRepeatedMethods(printer, isBuilder, /* builderName = */ "Builder" + className);
    }
}

FieldGenerator::FieldGenerator(FieldDescriptor const * descriptor)
        : descriptor(descriptor)
        , modifier(descriptor->label())
        , simpleName(descriptor->name())
        , underlyingType(protobufToKotlinType())
        , fullType(protobufToKotlinField())
        , initValue(getInitValue())
{ }

void FieldGenerator::generateSerializationCode(io::Printer *printer, bool isRead, bool noTag) const {
    map <string, string> vars;
    vars["type"] = protobufTypeToKotlinFunctionSuffix(descriptor->type());
    vars["fieldNumber"] = std::to_string(descriptor->number());
    vars["fieldName"] = simpleName;
    vars["arg"] = isRead ? "input" : "output";

    /**
     * First of all, try to generate syntax for repeated fields because it's separate case.
     * Do this according to protobuf format:
     * - Check if size of array is > 0, because empty repeated fields shouldn't appear in message
     * - Write tag explicitly
     * - Write length as int32 (note that tag shouldn't be added)
     * - Write all repeated elements via recursive call
     */
    if (modifier == FieldDescriptor::LABEL_REPEATED) {
        printer->Print(vars, "if ($fieldName$.size > 0) {\n");
        printer->Indent();

        // tag
        if (isRead) {
            //TODO: dirty stub here! Normally, reading from input should be delegated to Parsers, with proper error handling and etc.
            //Currently tag is ignored, and work of the library relies heavily on the field order guarantees.
            //Thus, backward-compability and extensions are not supported.
            printer->Print(vars, "val tag = input.readTag($fieldNumber$, WireType.LENGTH_DELIMITED)\n");
            printer->Print(vars, "val listSize = input.readInt32NoTag()\n");
            printer->Print(vars, "for (i in 1..listSize) {\n");
            printer->Indent();
            printer->Print(vars, "$fieldName$[i - 1].mergeFrom(input)\n");
            printer->Outdent();
            printer->Print("}\n");
        }
        else {
            // tag
            printer->Print(vars, "output.writeTag($fieldNumber$, WireType.LENGTH_DELIMITED)\n");

            // length
            printer->Print(vars, "output.writeInt32NoTag($fieldName$.size)\n");
            printer->Print(vars, "output.writeInt32NoTag($fieldName$.size)\n");

            // all elements
            printer->Print(vars, "for (item in $fieldName$) {\n");
            printer->Indent();

            /* hack: copy current FieldGenerator and change label to OPTIONAL. This will allow
               to re-use this function for generating serialization code for elements of array.
               More importantly, this will care about nested types too.
               Efficiently, it inlines serialization code for all underlying types.
               This hack isn't necessary from the architectural point of view and could be safely5
               removed as soon as target code will support inheritance and interfaces.
               (then writing CodedOutputStream.writeMessage will be possible).
             */
            FieldGenerator singleFieldGen = FieldGenerator(descriptor);
            singleFieldGen.modifier = FieldDescriptor::LABEL_OPTIONAL;
            singleFieldGen.generateSerializationCode(printer, isRead, /* noTag = */ true);

            printer->Outdent(); // for-loop
            printer->Print("}\n");
        }

        printer->Outdent(); // if-clause
        printer->Print("}\n");

        return;
    }

    /*
      Then check for conversions 'int -> enum-value' and \enum-value -> int' if current
      field is enum.
      This is necessary, because CodedStream stores enums as Ints in wire, delegating
      responsibility for casting those Ints to enum values and vice versa to the caller.
      Example: enumField = fromIntToMyEnumName(input.readEnum(42))
      Example: output.writeEnum(42, enumField.ord)
     */
    if (descriptor->type() == FieldDescriptor::TYPE_ENUM) {
        vars["converter"] = underlyingType + ".fromIntTo" + underlyingType;
        if (isRead) {
            printer->Print(vars, "$fieldName$ = $converter$(input.read$type$($fieldNumber$))\n");
        }
        else {
            printer->Print(vars, "output.write$type$ ($fieldNumber$, $fieldName$?.ord)\n");
        }
        return;
    }

    /*
      Then check for nested messages. Then we re-use writeTo method, that should be defined in
      that message
     */
    if (descriptor->type() == FieldDescriptor::TYPE_MESSAGE) {
        vars["fieldName"] = noTag ? "item" : simpleName;
        vars["maybeNoTag"] = noTag ? "NoTag" : "";
        vars["fieldNumber"] = std::to_string(descriptor->number());
        if (isRead) {
            printer->Print(vars,
                           "val tag = input.readTag($fieldNumber$, WireType.LENGTH_DELIMITED)\n"
                           "$fieldName$.readFrom$maybeNoTag$(input)\n");
        }
        else {
            printer->Print(vars,
                           "output.writeTag($fieldNumber$, WireType.LENGTH_DELIMITED)\n"
                           "$fieldName$.writeTo$maybeNoTag$(output)\n");
        }
        return;
    }

    /* Finally, serialize trivial cases    */
    if (isRead) {
        printer->Print(vars, "$fieldName$ = input.read$type$($fieldNumber$)\n");
    }
    else {
        printer->Print(vars, "output.write$type$ ($fieldNumber$, $fieldName$)\n");
    }
}

string FieldGenerator::protobufTypeToKotlinFunctionSuffix(FieldDescriptor::Type type) const {
    switch (type) {
        case FieldDescriptor::TYPE_DOUBLE:
            return "Double";
        case FieldDescriptor::TYPE_FLOAT:
            return "Float";
        case FieldDescriptor::TYPE_INT64:
            return "Int64";
        case FieldDescriptor::TYPE_UINT64:
            return "UInt64";
        case FieldDescriptor::TYPE_INT32:
            return "Int32";
        case FieldDescriptor::TYPE_FIXED64:
            return "Fixed64";
        case FieldDescriptor::TYPE_FIXED32:
            return "Fixed32";
        case FieldDescriptor::TYPE_BOOL:
            return "Bool";
        case FieldDescriptor::TYPE_STRING:
            return "String";
        case FieldDescriptor::TYPE_MESSAGE:
            return "Message";
        case FieldDescriptor::TYPE_BYTES:
            return "Bytes";
        case FieldDescriptor::TYPE_UINT32:
            return "UInt32";
        case FieldDescriptor::TYPE_ENUM:
            return "Enum";
        case FieldDescriptor::TYPE_SFIXED32:
            return "SFixed32";
        case FieldDescriptor::TYPE_SFIXED64:
            return "SFixed64";
        case FieldDescriptor::TYPE_SINT32:
            return "SInt32";
        case FieldDescriptor::TYPE_SINT64:
            return "SInt64";
    }
}

void FieldGenerator::generateSetter(io::Printer *printer, string builderName) const {
    map <string, string> vars;
    vars["camelCaseName"] = name_resolving::makeFirstLetterUpper(simpleName);
    vars["fieldName"] = simpleName;
    vars["builderName"] = builderName;
    vars["type"] = fullType;
    printer->Print(vars,
                    "fun set$camelCaseName$(value: $type$): $builderName$ {\n");
    printer->Indent();
    printer->Print(vars,
                    "$fieldName$ = value\n"
                    "return this\n");
    printer->Outdent();
    printer->Print("}\n");
}

void FieldGenerator::generateRepeatedMethods(io::Printer * printer, bool isBuilder, string builderName) const {
    map <string, string> vars;
    vars["elementType"] = underlyingType;
    vars["arg"] = "value";
    vars["fieldName"] = simpleName;
    vars["builderName"] = builderName;

    // generate indexed setter for builders
    if (isBuilder) {
        printer->Print(vars, "fun set$elementType$(index: Int, $arg$: $elementType$): $builderName$ {\n");
        printer->Indent();
        printer->Print(vars, "$fieldName$[index] = $arg$\n");
        printer->Print(vars, "return this\n");
        printer->Outdent();
        printer->Print("}\n");
    }

    #ifndef KOTLIN_GENERATED_CODE_LANGUAGE_LEVEL_LOW
    if (isBuilder) {
        // generate single-add for builders
        printer->Print(vars, "fun add$elementType$($arg$: $elementType$): $builderName$ {\n");
        printer->Indent();
        printer->Print(vars, "$fieldName$.add($arg$)\n"
                             "return this\n");
        printer->Outdent();
        printer->Print(vars, "}\n");

        // generate addAll for builders
        printer->Print(vars, "fun addAll$elementType$($arg$: Iterable<$elementType$>): $builderName$ {\n");
        printer->Indent();
        printer->Print(vars, "for (item in $arg$) {\n");
        printer->Indent();
        printer->Print(vars, "$fieldName$.add(item)\n");

        printer->Outdent();     // for-loop
        printer->Print("}\n");

        printer->Print("return this\n");

        printer->Outdent();     // function body
        printer->Print("}\n");
    }
    #endif
}


} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google