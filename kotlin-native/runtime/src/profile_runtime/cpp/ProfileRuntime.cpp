// Define symbols that are required for code coverage but missing in compiler-RT for MinGW. 
// See https://reviews.llvm.org/D58106/ for details.
#ifdef KONAN_WINDOWS

#include <windows.h>

extern "C" {

__attribute__((used))
int lprofGetHostName(char *hostName, int length) {
  const int maxHostNameLength = 128;
  WCHAR buffer[maxHostNameLength];
  DWORD bufferSize = sizeof(buffer);
  COMPUTER_NAME_FORMAT nameType = ComputerNameDnsFullyQualified;
  if (!GetComputerNameExW(nameType, buffer, &bufferSize)) {
    return -1;
  }
  int bytesWritten = WideCharToMultiByte(CP_UTF8, 0, buffer, -1, hostName, length, nullptr, nullptr);
  if (bytesWritten == 0) {
    return -1;
  } else {
    return 0;
  }
}

}

#endif
