
public class LCG {
	
	long mod = 1L << 32;
	long mul = 1103515245;
	long inc = 12345;
	long prev;
	
	LCG(long seed) {
		prev = seed;
	}
	
	int next() {
		int ret = (int)(prev >> 16) & 0x7fff;
		prev = (mul * prev + inc) % mod;
		return ret;
	}
	
}
