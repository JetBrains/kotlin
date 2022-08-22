#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

void setupSignalHandler(void);
void signalThread(uint64_t thread, int value);
int getValue(void);

#ifdef __cplusplus
}
#endif
