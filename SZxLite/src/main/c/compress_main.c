#include <stdio.h>
#include "szx.h"
#include "rocc.h"
#include "szx_rocc.h"
#include "test_data.h"
#include <stdint.h>
#include <stdlib.h> // Required for malloc and free
#include <string.h> // Required for memcpy

// Include necessary SZx definitions (only if not already defined)
#ifndef SZx_VER_MAJOR
#define SZx_VER_MAJOR 2
#endif
#ifndef SZx_VER_MINOR
#define SZx_VER_MINOR 1
#endif

// Declare the software version function
unsigned char* SZx_compress_float(float *oriData, size_t *outSize, float absErrBound, size_t nbEle, int blockSize);

// Macro to convert float to string
#define FLOAT_TO_STR(x) #x

// Real software compression function using SZx algorithm
unsigned char* real_software_compress(float *data, size_t nbEle, float errBound, int blockSize, size_t *outSize) {
    // Use the real SZx software implementation
    return SZx_compress_float(data, outSize, errBound, nbEle, blockSize);
}

// Simple software compression function for baseline
unsigned char* simple_software_compress(float *data, size_t nbEle, size_t *outSize) {
    // Simple implementation: just copy data with minimal processing
    unsigned char* output = malloc(nbEle * sizeof(float));
    if (!output) return NULL;

    // Copy data as-is (no actual compression, just for timing comparison)
    memcpy(output, data, nbEle * sizeof(float));
    *outSize = nbEle * sizeof(float);

    return output;
}

// RISC-V performance counter functions
static inline uint64_t read_cycle() {
    uint64_t cycles;
    asm volatile ("rdcycle %0" : "=r" (cycles));
    return cycles;
}

static inline uint64_t read_instret() {
    uint64_t instret;
    asm volatile ("rdinstret %0" : "=r" (instret));
    return instret;
}

// Enhanced profiling structure
typedef struct {
    uint64_t total_cycles;
    uint64_t total_instructions;
    uint64_t block_processing_cycles;
    uint64_t memory_access_cycles;
    uint64_t memory_reads;
    uint64_t memory_writes;
    uint64_t cache_misses;
} profile_data_t;

/*main*/
int main(void)
{
    int blockSize = 64;
    float errBound = 1E-3;

    printf("=== SZx RISC-V Test: Block=%d, Error=%s ===\n", blockSize, FLOAT_TO_STR(1E-3));
    printf("Using embedded test data (%lu elements)\n", (unsigned long)test_data_size);
    printf("Block size: %d\n", blockSize);
    printf("Error bound: %s\n", FLOAT_TO_STR(1E-3));
    printf("Starting compression...\n");

    // Start overall performance counters
    uint64_t start_cycles = read_cycle();
    uint64_t start_instret = read_instret();
    size_t nbEle = test_data_size;

    // Use heap allocation for the data buffer (Gemmini-style, Chipyard-compatible)
    float *data = malloc(nbEle * sizeof(float));
    if (!data) {
        printf("Error: failed to allocate data buffer!\n");
        return 1;
    }
    printf("Allocated data at address: 0x%lx\n", (unsigned long)data);

    // Data loading profiling (separate from compression)
    uint64_t data_load_start_cycles = read_cycle();
    uint64_t mem_reads = 0;
    uint64_t mem_writes = 0;

    for (size_t i = 0; i < nbEle; i++) {
            data[i] = test_data[i];
            mem_reads++;  // Count test_data reads
            mem_writes++; // Count data writes
    }
    uint64_t data_load_end_cycles = read_cycle();
    uint64_t data_load_cycles = data_load_end_cycles - data_load_start_cycles;

    // Compression profiling (separate from data loading)
    uint64_t compression_start_cycles = read_cycle();
    uint64_t compression_start_instret = read_instret();

    size_t outSize;

    // Simple flag to switch between hardware and software
    int use_hardware = 1; // Set to 1 for hardware, 0 for software

    // Set the global flag that controls hardware/software selection
    extern int g_use_hardware_acceleration;
    g_use_hardware_acceleration = use_hardware;

    unsigned char* bytes;
    if (use_hardware) {
        printf("Using RoCC hardware acceleration\n");
        bytes = SZx_compress_float_rocc(data, &outSize, errBound, nbEle, blockSize); // Hardware
    } else {
        // Use the flag-controlled software implementation
        printf("Using software SZx implementation\n");
        bytes = SZx_compress_float(data, &outSize, errBound, nbEle, blockSize);
    }

    // Compression profiling - end
    uint64_t compression_end_cycles = read_cycle();
    uint64_t compression_end_instret = read_instret();
    uint64_t compression_cycles = compression_end_cycles - compression_start_cycles;
    uint64_t compression_instructions = compression_end_instret - compression_start_instret;

    // End overall performance counters
    uint64_t end_cycles = read_cycle();
    uint64_t end_instret = read_instret();

    // Calculate compression ratio
    unsigned long total_size = nbEle * sizeof(float);
    unsigned long cr_numerator = total_size * 1000000;
    unsigned long cr_denominator = outSize;
    unsigned long cr_integer = cr_numerator / cr_denominator;

    printf("compression size = %lu, CR = %lu.%06lu\n",
           (unsigned long)outSize,
           cr_integer / 1000000,
           cr_integer % 1000000);

    // Enhanced performance results (fixed measurements)
    uint64_t total_cycles = end_cycles - start_cycles;
    uint64_t total_instructions = end_instret - start_instret;
    // data_load_cycles, compression_cycles, and compression_instructions already calculated above

    // Calculate CPI values
    uint64_t cpi_numerator = total_cycles * 100;
    uint64_t cpi_integer = cpi_numerator / total_instructions;
    uint64_t compression_cpi_numerator = compression_cycles * 100;
    uint64_t compression_cpi_integer = compression_cpi_numerator / compression_instructions;

    // Calculate cycles per element
    uint64_t cycles_per_element = total_cycles / nbEle;
    uint64_t compression_cycles_per_element = compression_cycles / nbEle;

    // Calculate number of blocks
    uint64_t num_blocks = (nbEle + blockSize - 1) / blockSize;
    uint64_t cycles_per_block = compression_cycles / num_blocks;

    printf("=== Enhanced Performance Results ===\n");
    printf("Total cycles: %lu\n", total_cycles);
    printf("Total instructions: %lu\n", total_instructions);
    printf("CPI (Cycles per Instruction): %lu.%02lu\n",
           cpi_integer / 100, cpi_integer % 100);
    printf("Cycles per element: %lu\n", cycles_per_element);
    printf("\n=== Compression Breakdown ===\n");
    printf("Data loading cycles: %lu\n", data_load_cycles);
    printf("Compression cycles: %lu\n", compression_cycles);
    printf("Compression instructions: %lu\n", compression_instructions);
    printf("Compression CPI: %lu.%02lu\n", compression_cpi_integer / 100, compression_cpi_integer % 100);
    printf("Cycles per element (compression only): %lu\n", compression_cycles_per_element);
    printf("Number of blocks: %lu\n", num_blocks);
    printf("Cycles per block: %lu\n", cycles_per_block);
    printf("\n=== Memory Access Patterns ===\n");
    printf("Memory reads: %lu\n", mem_reads);
    printf("Memory writes: %lu\n", mem_writes);
    printf("Data loading percentage: %lu%%\n", (data_load_cycles * 100) / total_cycles);
    printf("Compression percentage: %lu%%\n", (compression_cycles * 100) / total_cycles);

    printf("\nCompression completed successfully!\n");
    free(data);
    free(bytes);
    printf("done\n");
    return 0;
}
