//
// Created by user on 7/14/16.
//

#include "kotlin_name_resolver.h"
#include <string>

namespace google {
namespace protobuf {
namespace compiler {
namespace kotlin {

namespace name_resolving {

using namespace std;

string makeFirstLetterUpper(std::string s) {
    s[0] = std::toupper(s[0]);
    return s;
}

string getFileNameWithoutExtension(string fullName) {
    size_t file_extension_index = fullName.find_last_of(".");
    return fullName.substr(0, file_extension_index);
}

string getKotlinOutputByProtoName(string protoName) {
    string justName = getFileNameWithoutExtension(protoName);
    return justName + ".kt";
}

} // namespace name_resolving
} // namespace kotlin
} // namespace compiler
} // namespace protobuf
} // namespace google