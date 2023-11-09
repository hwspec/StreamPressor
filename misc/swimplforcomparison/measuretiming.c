#include <stdio.h>
#include <stdint.h>
#include <rdtsc.h>

volatile uint32_t dummy = 0;

static uint32_t int2fp(float a)
{
  uint32_t b = *(uint32_t*)&a;

  if (b&0x80000000)  return b&0x7fffffff;
  else return ~b;
}

static void bitshuffle_16_32b(const uint32_t *in, uint16_t *out)
{
  for (int i=0; i<16; i++) {
	for (int j=0; j<32; j++) {
	  uint32_t mask = 1<<j;
	  uint32_t bit = (in[i]&mask)>>j;
	  out[i] |= bit << i;
	}
  }
}

static double bench_bitshuffle()
{
  uint64_t st, et;
  int n = 1000000;
  uint32_t in[16];
  uint16_t out[32];

  for (int i=0; i<16; i++) {
	in[i] = i;
  }

  st = rdtsc();
  for (int i=0; i<n; i++) {
	bitshuffle_16_32b(in, out);
  }
  et = rdtsc() - st;

  return  et/(double)n;
}



static double bench_intergerizedfp()
{
  uint64_t st, et;
  int n = 1000000;
  uint32_t c = 0;

  st = rdtsc();
  for (int i=0; i<n; i++) {
	float a=(float)i;
	//uint32_t b = *(uint32_t*)&a;
	//if (b&0x80000000) c+=b&0x7fffffff;
	//else c+=~b;
	c += int2fp(a);
  }
  et = rdtsc() - st;

  dummy = c;
  return  et/(double)n;
}


// return the average cycle per operation
static double bench_clz()
{
  uint64_t st, et;
  uint32_t i, j, clz=0;
  uint32_t n = 1000000;
  const int unrole = 10;

  st = rdtsc();
  for (i=0; i<n; i++) {
	for (j=0; j<unrole; j++)
	  clz += __builtin_clz(i); // 1 uop with 3 cycle latency and 1/clock throughput. no need to benchmark
  }
  et = rdtsc() - st;
  dummy = clz;
  return  et/(double)n/(double)unrole;
}

int main()
{
  printf("clz:   %lf cycles/op\n", bench_clz());
  printf("ifp:   %lf cycles/op\n", bench_intergerizedfp());
  printf("bs:    %lf cycles/op\n", bench_bitshuffle());

  return 0;
}
