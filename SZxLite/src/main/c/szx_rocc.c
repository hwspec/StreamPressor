#include "szx_rocc.h"
#include "rocc.h"
#include <stdio.h>
#include <stdlib.h>

// Hardware-accelerated compression function using RoCC with scratchpad
unsigned char* SZx_compress_float_rocc(float *oriData, size_t *outSize, float absErrBound, size_t nbEle, int blockSize) {
    printf("Using RoCC hardware acceleration with scratchpad!\n");

    // Allocate output buffer (worst case: 4 bytes per float)
    unsigned char* outputBytes = (unsigned char*)malloc(nbEle * 4);
    if (!outputBytes) {
        printf("Error: failed to allocate output buffer!\n");
        return NULL;
    }

    // Calculate number of blocks
    size_t numBlocks = (nbEle + blockSize - 1) / blockSize;
    size_t totalCompressedSize = 0;

    printf("Processing %lu blocks with RoCC accelerator...\n", (unsigned long)numBlocks);

    // Process each block using RoCC accelerator
    for (size_t block = 0; block < numBlocks; block++) {
        // Progress indicator every 10 blocks
        if (block % 10 == 0) {
            printf("Processing block %lu/%lu...\n", (unsigned long)(block + 1), (unsigned long)numBlocks);
            printf("Using bulk loading for block %lu (64 elements)\n", (unsigned long)block);
        }

        size_t blockStart = block * blockSize;
        size_t blockEnd = (blockStart + blockSize < nbEle) ? blockStart + blockSize : nbEle;
        size_t blockEle = blockEnd - blockStart;

        // Calculate median and radius for this block
        float min = oriData[blockStart];
        float max = oriData[blockStart];
        for (size_t i = blockStart + 1; i < blockEnd; i++) {
            if (oriData[i] < min) min = oriData[i];
            if (oriData[i] > max) max = oriData[i];
        }
        float radius = (max - min) / 2.0f;
        float medianValue = min + radius;

        // Configure RoCC accelerator for this block
        szx_config(absErrBound, medianValue);
        szx_set_radius(radius);

        // Use bulk loading instead of individual data transfers
        // This eliminates 64 individual RoCC calls per block
        szx_load_block_bulk((uint32_t)(oriData + blockStart), blockEle);

        // Call compression function
        uint32_t compressedSize = szx_compress(0, 0); // Dummy addresses, data is in scratchpad

        // Only print results for first few blocks and every 10th block
        if (block < 5 || block % 10 == 0) {
            printf("Block %lu: %lu elements, compressed to %u bytes\n", (unsigned long)block, (unsigned long)blockEle, compressedSize);
        }

        totalCompressedSize += compressedSize;
    }

    printf("All blocks processed. Total compressed size: %lu bytes\n", (unsigned long)totalCompressedSize);
    *outSize = totalCompressedSize;
    return outputBytes;
}
