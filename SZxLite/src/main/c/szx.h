#include "utility.h"

void floatToBytes(unsigned char *b, float num);
void sizeToBytes(unsigned char* outBytes, size_t size);
void computeReqLength_float(double realPrecision, short radExpo, int *reqLength, float *medianValue);
short getPrecisionReqLength_double(double precision);
short getExponent_float(float value);
void longToBytes_bigEndian(unsigned char *b, unsigned long num);
size_t bytesToSize(unsigned char* bytes);
void sizeToBytes(unsigned char* outBytes, size_t size);
long bytesToLong_bigEndian(unsigned char* b);
float bytesToFloat(unsigned char* bytes);

size_t computeStateMedianRadius_float(float *oriData, size_t nbEle, float absErrBound, int blockSize,
                                      unsigned char *stateArray, float *medianArray, float *radiusArray);

size_t convertIntArray2ByteArray_fast_1b_args(unsigned char* intArray, size_t intArrayLength, unsigned char *result);
size_t convertIntArray2ByteArray_fast_2b_args(unsigned char* timeStepType, size_t timeStepTypeLength, unsigned char *result);


void convertByteArray2IntArray_fast_2b(size_t stepLength, unsigned char* byteArray, size_t byteArrayLength, unsigned char **intArray);
void convertByteArray2IntArray_fast_1b_args(size_t intArrayLength, unsigned char* byteArray, size_t byteArrayLength, unsigned char* intArray);

void SZx_compress_one_block_float(float *oriData, size_t nbEle, float absErrBound,
                                                                unsigned char *outputBytes, int *outSize,
                                                                unsigned char *leadNumberArray_int, float medianValue,
                                                                float radius);

int SZx_decompress_one_block_float(float* newData, size_t blockSize, unsigned char* cmpBytes);

unsigned char *SZx_compress_float(float *oriData, size_t *outSize, float absErrBound, size_t nbEle, int blockSize);
void SZx_decompress_float(float** newData, size_t nbEle, unsigned char* cmpBytes);
