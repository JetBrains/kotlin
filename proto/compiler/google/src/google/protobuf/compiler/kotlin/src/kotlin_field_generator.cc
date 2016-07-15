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

string FieldGenerator::getInitValue() const {
    if (descriptor->is_repeated())
    #ifndef KOTLIN_GENERATED_CODE_LANGUAGE_LEVEL_LOW
        return "mutableListOf()";
    #else
        return "arrayOf()";
    #endif

    return name_resolving::protobufTypeToInitValue(descriptor);
}

void FieldGenerator::generateCode(io::Printer *printer, bool isBuilder) const {
    map<string, string> vars;
    vars["name"] = simpleName;
    vars["field"] = name_resolving::protobufToKotlinField(descriptor);
    printer->Print(vars, "var $name$ : $field$\n");

    // make setter private
    printer->Indent();
    printer->Print("private set\n");
    printer->Outdent();

    // generate setter for builder
    if (isBuilder) {
        generateSetter(printer);
    }

    // generate additional methods for repeated fields
    if (modifier == FieldDescriptor::LABEL_REPEATED) {
        generateRepeatedMethods(printer, isBuilder);
    }
}

FieldGenerator::FieldGenerator(FieldDescriptor const * descriptor, ClassGenerator const * enclosingClass)
        : descriptor(descriptor)
        , modifier(descriptor->label())
        , enclosingClass(enclosingClass)
        , simpleName(descriptor->name())
        , underlyingType(name_resolving::protobufToKotlinType(descriptor))
        , fullType(name_resolving::protobufToKotlinField(descriptor))
        , protoType(descriptor->type())
        , fieldNumber(descriptor->number())
{ }

// TODO: long, complicated and messy method. Refactor it ASAP
void FieldGenerator::generateSerializationCode(io::Printer *printer, bool isRead, bool noTag) const {
    map <string, string> vars;
    vars["type"] = name_resolving::protobufTypeToKotlinFunctionSuffix(descriptor->type()) + (noTag ? "NoTag" : "");
    vars["fieldNumber"] = std::to_string(fieldNumber);
    vars["maybeFieldNumber"] = noTag ? "" : std::to_string(fieldNumber);
    vars["fieldName"] = simpleName;
    vars["arg"] = isRead ? "input" : "output";
    vars["maybeComma"] = ", ";

    /**
     * First of all, try to generate syntax for repeated fields because it's separate case.
     * Do this according to protobuf format:
     * - Check if size of array is > 0, because empty repeated fields shouldn't appear in message
     * - Write tag explicitly
     * - Write length as int32 (note that tag shouldn't be added)
     * - Write all repeated elements via recursive call (again, without tags)
     */
    if (modifier == FieldDescriptor::LABEL_REPEATED) {
        // tag
        if (isRead) {
            if (!noTag) {
                printer->Print(vars, "val tag = input.readTag($fieldNumber$, WireType.LENGTH_DELIMITED)\n");
            }
            printer->Print(vars, "val listSize = input.readInt32NoTag()\n");
            printer->Print(vars, "for (i in 1..listSize) {\n");
            printer->Indent();

            /* hack: copy current FieldGenerator and change label to OPTIONAL. Also change name to
               name of iterator in for-loop.
               This will allow to re-use this function for generating serialization code for elements of array.
               More importantly, this will care about nested types too.
               Efficiently, it inlines serialization code for all underlying types.
               This hack isn't necessary from the architectural point of view and could be safely
               removed as soon as target code will support inheritance and interfaces.
               (then writing CodedOutputStream.writeMessage will be possible).
             */
            FieldGenerator singleFieldGen = FieldGenerator(descriptor, enclosingClass);

            /* Another dirty hack here: create tmp variable of a given type and read it from input stream
               then add that tmp var into list.
               This is made because simple recursive call will generate code, that tries to array[i].mergeFrom()
               but this is incorrect, because array has old size, while 'i' iterates over new size, and of course
               they are not necessary the same.
            */
            // TODO: stub here, resolve name properly!
            vars["fieldType"] = underlyingType + ".Builder" + underlyingType;
            vars["initValue"] = getUnderlyingTypeInitValue();
            printer->Print(vars, "val tmp: $fieldType$ = $fieldType$()\n");
            singleFieldGen.simpleName = "tmp";
            singleFieldGen.modifier = FieldDescriptor::LABEL_OPTIONAL;
            singleFieldGen.generateSerializationCode(printer, isRead, /* noTag = */ true);

            printer->Print(vars, "$fieldName$.add(tmp.build())\n");

            printer->Outdent();
            printer->Print("}\n");
        }
        else {
            printer->Print(vars, "if ($fieldName$.size > 0) {\n");
            printer->Indent();

            // tag
            printer->Print(vars, "output.writeTag($fieldNumber$, WireType.LENGTH_DELIMITED)\n");

            // length
            printer->Print(vars, "output.writeInt32NoTag($fieldName$.size)\n");

            // all elements
            printer->Print(vars, "for (item in $fieldName$) {\n");
            printer->Indent();

            // hack: see above
            FieldGenerator singleFieldGen = FieldGenerator(descriptor, enclosingClass);
            singleFieldGen.simpleName = "item";
            singleFieldGen.modifier = FieldDescriptor::LABEL_OPTIONAL;
            singleFieldGen.generateSerializationCode(printer, isRead, /* noTag = */ true);

            printer->Outdent(); // for-loop
            printer->Print("}\n");

            printer->Outdent(); // if-clause
            printer->Print("}\n");
        }
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
            printer->Print(vars, "$fieldName$ = $converter$(input.read$type$($maybeFieldNumber$))\n");
        }
        else {
            printer->Print(vars, "output.write$type$ ($maybeFieldNumber$$maybeComma$$fieldName$?.ord)\n");
        }
        return;
    }

    /*
      Then check for nested messages. Then we re-use writeTo method, that should be defined in
      that message
     */
    if (descriptor->type() == FieldDescriptor::TYPE_MESSAGE) {
        vars["maybeNoTag"] = "NoTag";
        if (isRead) {
            printer->Print(vars,
                           "$fieldName$.readFrom$maybeNoTag$(input)\n");
        }
        else {
            printer->Print(vars,
                           "$fieldName$.writeTo$maybeNoTag$(output)\n");
        }
        return;
    }

    /* Finally, serialize trivial cases    */
    if (isRead) {
        printer->Print(vars, "$fieldName$ = input.read$type$($maybeFieldNumber$)\n");
    }
    else {
        printer->Print(vars, "output.write$type$ ($maybeFieldNumber$$maybeComma$$fieldName$)\n");
    }
}


void FieldGenerator::generateSetter(io::Printer *printer) const {
    map <string, string> vars;
    vars["camelCaseName"] = name_resolving::makeFirstLetterUpper(simpleName);
    vars["fieldName"] = simpleName;
    vars["builderName"] = enclosingClass->builderName;
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

void FieldGenerator::generateRepeatedMethods(io::Printer * printer, bool isBuilder) const {
    map <string, string> vars;
    vars["elementType"] = underlyingType;
    vars["arg"] = "value";
    vars["fieldName"] = simpleName;
    vars["builderName"] = enclosingClass->builderName;

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

string FieldGenerator::getKotlinFunctionSuffix() const {
    return name_resolving::protobufTypeToKotlinFunctionSuffix(descriptor->type());
}

string FieldGenerator::getUnderlyingTypeInitValue() const {
    return name_resolving::protobufTypeToInitValue(descriptor);
}

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google