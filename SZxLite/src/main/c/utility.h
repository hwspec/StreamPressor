#ifndef _UTILITY_H
#define _UTILITY_H

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include "define.h"

#ifdef _WIN32
#define PATH_SEPARATOR ';'
#else
#define PATH_SEPARATOR ':'
#endif

#ifdef __cplusplus
extern "C" {
#endif


void writeByteData(unsigned char *bytes, size_t byteLength, char *tgtFilePath, int *status);
void writeFloatData_inBytes(float *data, size_t nbEle, char* tgtFilePath, int *status);
float *readFloatData(char *srcFilePath, size_t *nbEle, int *status);
unsigned char *readByteData(char *srcFilePath, size_t *byteLength, int *status);

#ifdef __cplusplus
}
#endif

#endif /* ----- #ifndef _UTILITY_H  ----- */
