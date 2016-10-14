#ifndef RUNTIME_ASSERT_H
#define RUNTIME_ASSERT_H

void RuntimeAssertFailed(const char* location, const char* message);

#define STRINGIFY(x) #x
#define TOSTRING(x) STRINGIFY(x)

#define RuntimeAssert(condition, message) \
  if (!(condition)) {                        \
    RuntimeAssertFailed( __FILE__ ":" TOSTRING(__LINE__), message); \
  }

#endif // RUNTIME_ASSERT_H
