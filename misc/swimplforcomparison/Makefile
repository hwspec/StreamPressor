
CFLAGS=-O3 -Wall -Wno-strict-aliasing -I.

all: measuretiming disasmtest.s

measuretiming: measuretiming.c disasmtest.c

disasmtest.s: disasmtest.c
	$(CC) $(CFLAGS) -S $<

clean:
	rm -f measuretiming
	rm -f *.s
