#include "define.h"
#include "utility.h"
#include <time.h>


//write compressed data in bytes
void writeByteData(unsigned char *bytes, size_t byteLength, char *tgtFilePath, int *status)
{
        FILE *pFile = fopen(tgtFilePath, "wb");
    if (pFile == NULL)
    {
        printf("Failed to open input file. 3\n");
        *status = SZx_FERR;
        return;
    }

    fwrite(bytes, 1, byteLength, pFile); //write outSize bytes
    fclose(pFile);
    *status = SZx_SCES;
}

//write float data in bytes (e.g., write decompressed data into a file)
void writeFloatData_inBytes(float *data, size_t nbEle, char* tgtFilePath, int *status)
{
        size_t i = 0;
        int state = SZx_SCES;
        lfloat buf;
        unsigned char* bytes = (unsigned char*)malloc(nbEle*sizeof(float));
        for(i=0;i<nbEle;i++)
        {
                buf.value = data[i];
                bytes[i*4+0] = buf.byte[0];
                bytes[i*4+1] = buf.byte[1];
                bytes[i*4+2] = buf.byte[2];
                bytes[i*4+3] = buf.byte[3];
        }

        size_t byteLength = nbEle*sizeof(float);
        writeByteData(bytes, byteLength, tgtFilePath, &state);
        free(bytes);
        *status = state;
}

//read float data from binary-format file
float *readFloatData(char *srcFilePath, size_t *nbEle, int *status)
{
        size_t inSize;
        FILE *pFile = fopen(srcFilePath, "rb");
    if (pFile == NULL)
    {
        printf("Failed to open input file. 1\n");
        *status = SZx_FERR;
        return NULL;
    }
        fseek(pFile, 0, SEEK_END);
    inSize = ftell(pFile);
    *nbEle = inSize/4;
    fclose(pFile);

    if(inSize<=0)
    {
                printf("Error: input file is wrong!\n");
                *status = SZx_FERR;
        }

    float *daBuf = (float *)malloc(inSize);

    pFile = fopen(srcFilePath, "rb");
    if (pFile == NULL)
    {
        printf("Failed to open input file. 2\n");
        *status = SZx_FERR;
        return NULL;
    }
    fread(daBuf, 4, *nbEle, pFile);
    fclose(pFile);
    *status = SZx_SCES;
    return daBuf;
}

unsigned char *readByteData(char *srcFilePath, size_t *byteLength, int *status)
{
        FILE *pFile = fopen(srcFilePath, "rb");
    if (pFile == NULL)
    {
        printf("Failed to open input file. 1\n");
        *status = SZx_FERR;
        return 0;
    }
        fseek(pFile, 0, SEEK_END);
    *byteLength = ftell(pFile);
    fclose(pFile);

    unsigned char *byteBuf = ( unsigned char *)malloc((*byteLength)*sizeof(unsigned char)); //sizeof(char)==1

    pFile = fopen(srcFilePath, "rb");
    if (pFile == NULL)
    {
        printf("Failed to open input file. 2\n");
        *status = SZx_FERR;
        return 0;
    }
    fread(byteBuf, 1, *byteLength, pFile);
    fclose(pFile);
    *status = SZx_SCES;
    return byteBuf;
}
