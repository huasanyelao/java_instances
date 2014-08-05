package org.chinaml.util;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

public class BloomFilter<E> implements Serializable {
	
	private BitSet bitset;
	private int bitSetSize;				// m
	private double bitsPerElement;		// m/n
	private int maxNum;					// n
	private int numOfAdded;				// numer of elements added to bloom filter
	private int k;						// k: number of hash functions
	
	static final Charset charset = Charset.forName("UTF-8");
	
	static final String hashFunc = "MD5";
	static final MessageDigest digestFunc;
	static {
			MessageDigest messageDigest;
			try {
				messageDigest = MessageDigest.getInstance(hashFunc);
			} catch (NoSuchAlgorithmException e) {
				messageDigest = null;
				e.printStackTrace();
			}
			digestFunc = messageDigest;
	}
	
	private BloomFilter(double c, int n, int k) {
		this.bitsPerElement = c;
		this.maxNum = n;
		this.k = k;
		this.bitSetSize = (int)Math.ceil(n * c);
		this.bitset = new BitSet(this.bitSetSize);
	}
	
	public BloomFilter(double falsePositiveRate, int n) {
		this(Math.ceil(-(Math.log(falsePositiveRate) / Math.log(2))) / Math.log(2), n, 
				(int)Math.ceil(-(Math.log(falsePositiveRate) / Math.log(2))));
		// c = k / ln(2)
		// k = ceil(-ln(p) / ln(2)) = ceil(-log_2(p))
	}
	
	public static int[] createHashes(byte[] data, int hashes) {
		int[] result = new int[hashes];
		
		int  k = 0;
		byte salt = 0;
		
		while (k < hashes) {
			byte[] digest;
			
			synchronized (digestFunc) {
				digestFunc.update(salt);
				salt++;
				digest = digestFunc.digest(data);
			}
			
			for (int i = 0; i < digest.length / 4 && k < hashes; i++) {
				int h = 0;
				for (int j = i * 4; j < (i + 1) * 4; j++) {
					h <<= 8;
					h |= ((int)digest[j]) & 0xFF;  // get 4 bytes, convert bytes to int
				}
				
				result[k] = h;
				k++;
			}
		}
		
		return result;
	}
	
	@Override
	public int hashCode() {
		int hash = 7;
		hash = 61 * hash + (this.bitset != null ? this.bitset.hashCode() : 0);
		hash = 61 * hash + this.maxNum;
		hash = 61 * hash + this.bitSetSize;
		hash = 61 * hash + this.k;
		
		return hash;
	}
	
	public double getExpectedFalsePositiveRate() {
		return getFalsePositiveRate(maxNum);
	}
	
	public double getFalsePositiveRate(double n) {
		return Math.pow(1 - Math.exp(-k * (n / bitSetSize)), k);
	}
	
	public int getK() {
		return k;
	}
	
	public void clear() {
		bitset.clear();
		numOfAdded = 0;
	}
	
	public void add(E element) {
		add(element.toString().getBytes(charset));
	}
	
	public void add(byte[] bytes) {
		int[] hashes = createHashes(bytes, k);
		for (int hash : hashes) {
			bitset.set(Math.abs(hash % bitSetSize), true);
		}
		
		numOfAdded++;
	}
	
	public boolean contains(E element) {
		return contains(element.toString().getBytes(charset));
	}
	
	public boolean contains(byte[] bytes) {
		int[] hashes = createHashes(bytes, k);
		for(int hash : hashes) {
			if (!bitset.get(Math.abs(hash % bitSetSize))) {
				return false;
			}
		}
		
		return true;
	}
	
	public int size() {
		return bitSetSize;
	}
	
	public int count() {
		return numOfAdded;
	}
}
