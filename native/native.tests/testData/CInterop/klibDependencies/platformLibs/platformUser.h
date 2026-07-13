#ifndef PLATFORM_USER_H
#define PLATFORM_USER_H

#include <pthread.h>
#include <CommonCrypto/CommonCrypto.h>

void lockMutex(pthread_mutex_t* mutex);
void decrypt(CC_SHA256_CTX* data);

#endif
