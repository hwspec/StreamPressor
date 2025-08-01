#ifndef _DEFINE_H
#define _DEFINE_H

#ifdef __cplusplus
extern "C" {
#endif

#define SZx_SCES 1
#define SZx_FERR 2
#define SZx_VER_MAJOR 0
#define SZx_VER_MINOR 1

typedef union lfloat
{
    float value;
    unsigned int ivalue;
    unsigned char byte[4];
} lfloat;

typedef union ldouble
{
    double value;
    unsigned long lvalue;
    unsigned char byte[8];
} ldouble;

#ifdef __cplusplus
}
#endif

#endif
