//
// Created by user on 7/14/16.
//

#ifndef GOOGLE_KOTLIN_NAME_RESOLVER_H
#define GOOGLE_KOTLIN_NAME_RESOLVER_H

#include <string>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {
namespace name_resolving {

std::string makeFirstLetterUpper(std::string s);

std::string getFileNameWithoutExtension(std::string fullName);

std::string getKotlinOutputByProtoName(std::string protoName);

} // namespace name_resolving
} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google


#endif //GOOGLE_KOTLIN_NAME_RESOLVER_H
