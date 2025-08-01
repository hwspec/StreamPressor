#ifndef SZX_ROCC_H
#define SZX_ROCC_H

#include <stddef.h>

// Hardware-accelerated compression function using RoCC
unsigned char* SZx_compress_float_rocc(float *oriData, size_t *outSize, float absErrBound, size_t nbEle, int blockSize);

#endif // SZX_ROCC_H
