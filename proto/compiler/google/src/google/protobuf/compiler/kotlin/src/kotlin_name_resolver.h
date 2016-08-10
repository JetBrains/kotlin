//
// Created by user on 7/14/16.
//

#ifndef GOOGLE_KOTLIN_NAME_RESOLVER_H
#define GOOGLE_KOTLIN_NAME_RESOLVER_H

#include <string>
#include <google/protobuf/descriptor.h>
#include <map>
#include "kotlin_field_generator.h"
#include "kotlin_class_generator.h"

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

class FieldGenerator;   // declared in kotlin_field_generator.h
class ClassGenerator;   // declared in kotlin_class_generator.h

class NameResolver {
public:
    NameResolver();

    void addClass (string simpleName, string parentName);
    void addGeneratorForClass (string simpleName, ClassGenerator * classGenerator);
    string getClassName (string simpleName);
    string getBuilderName (string classSimpleName);
    ClassGenerator * getClassGenerator (string simpleName);
private:
    std::map <string, string> names;
    std::map <string, string> builders;
    std::map <string, ClassGenerator *> generators;
};
namespace name_resolving {

std::string makeFirstLetterUpper(std::string s);

std::string getFileNameWithoutExtension(std::string fullName);

std::string getKotlinOutputByProtoName(std::string protoName);

std::string protobufTypeToInitValue(FieldDescriptor::Type type);

std::string protobufToKotlinType(FieldDescriptor::Type type);

std::string protobufTypeToKotlinWireType(FieldDescriptor::Type type);

/**
* Converts one of protobuf wire types to corresponding Kotlin type with proper
* naming, so it could be used as suffix after read/write, resulting in function
* in CodedInputStream/CodedOutputStream.
* Example: protobufToKotlinFunctionSuffix(TYPE_SFIXED32) returns "SFixed32", and
*          in Kotlin runtime exists method
*          CodedInputStream.readSFixed32(fieldNumber: Int)
*/
std::string protobufTypeToKotlinFunctionSuffix(FieldDescriptor::Type type);

} // namespace name_resolving
} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google


#endif //GOOGLE_KOTLIN_NAME_RESOLVER_H
