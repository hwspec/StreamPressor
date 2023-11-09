#include <stdio.h>
#include <stdint.h>
#include <rdtsc.h>

uint32_t int2fp(float a)
{
  uint32_t b = *(uint32_t*)&a;

  if (b&0x80000000)  return b&0x7fffffff;
  else return ~b;
}
