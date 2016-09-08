//
// Created by user on 7/12/16.
//

#ifndef PROTOBUF_KOTLIN_FIELD_GENERATOR_H
#define PROTOBUF_KOTLIN_FIELD_GENERATOR_H

#include <vector>
#include <google/protobuf/io/printer.h>
#include <google/protobuf/descriptor.h>
#include "kotlin_class_generator.h"
#include "kotlin_name_resolver.h"

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

class ClassGenerator;       // declared in kotlin_class_generator.h
class NameResolver;         // declared in kotlin_name_resolver.h

class FieldGenerator {
private:
    FieldDescriptor const * descriptor;

    void generateSetter(io::Printer * printer) const;
    void generateComment(io::Printer * printer) const;
    void generateRepeatedMethods(io::Printer * printer, bool isBuilder) const;

    void generateSerializationForPacked     (io::Printer * printer, bool isRead, bool noTag, bool isField) const;
    void generateSerializationForEnums      (io::Printer * printer, bool isRead, bool noTag, bool isField) const;
    void generateSerializationForMessages   (io::Printer * printer, bool isRead, bool noTag, bool isField) const;
    void generateSerializationForPrimitives (io::Printer * printer, bool isRead, bool noTag, bool isField) const;

    void generateSizeForPacked      (io::Printer * printer, string varName, bool noTag, bool isField) const;
    void generateSizeForEnums       (io::Printer * printer, string varName, bool noTag, bool isField) const;
    void generateSizeForMessages    (io::Printer * printer, string varName, bool noTag, bool isField) const;
    void generateSizeForPrimitives  (io::Printer * printer, string varName, bool noTag, bool isField) const;
public:
    ClassGenerator const * enclosingClass;    // class, in which that field is defined
    NameResolver * nameResolver;
    string simpleName;

    FieldDescriptor::Label protoLabel;  //  TODO: hack here - this field is used for some dark magic that allows us to drop generics from the generated code

    FieldDescriptor::Label getProtoLabel() const;
    FieldDescriptor::Type  getProtoType() const;

    /* Return declared tag number */
    int getFieldNumber() const;

    /* Returns instance of FieldGenerator, that generated underlying type for repeated fields.
     * For non-repeated fields, returns `this` */
    FieldGenerator getUnderlyingTypeGenerator() const;

    /* For repeated fields, returns simple name of single element.
     * For all other cases, returns simple name of field, which is the same as getType()
     */

    /* For repeated fields, return simple name of single element, wrapped into corresponding Kotlin array type
     *      Example: Array<NestedMessage>
     * For other types, return simple name (without full-qualification for non-primitive types) of field's type
     */
    string getSimpleType() const;

    string getBuilderSimpleType() const;

    /* Returns full=qualified name of builder if field type is user-defined message */
    string getBuilderFullType() const;

    /* Returns the same as getType(), but with full-qualification for non-primitive types if necessary.
     *      Example: Array<EnclosingMessage.NestedMessage>
     */
    string getFullType() const;

    /* Returns initial value of this field's type.
     * Note that full qualification for non-primitive types will always used here.
     */
    string getInitValue() const;


    /* Return string, that is suitable as suffix for corresponding IO methods in ProtoKot runtime.
     *      Example: int64-field -> Int64 (readInt64() and writeInt64() methods exist in ProtoKot runtime)
     */
    string getKotlinFunctionSuffix() const;

    /* Return function name in enum namespace that converts from enum to Int */
    string getEnumFromIntConverter() const;

    string getWireType() const;
    void generateCode(io::Printer * printer, bool isBuilder) const;
    void generateSerializationCode (io::Printer * printer, bool isRead = false, bool noTag = false, bool isField = true) const;
    void generateSizeEstimationCode(io::Printer * printer, string varName, bool noTag = false, bool isField = true) const;
    FieldGenerator(FieldDescriptor const * descriptor, ClassGenerator const * enclosingClass, NameResolver * nameResolver);
};

} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google

#endif //PROTOBUF_KOTLIN_FIELD_GENERATOR_H
