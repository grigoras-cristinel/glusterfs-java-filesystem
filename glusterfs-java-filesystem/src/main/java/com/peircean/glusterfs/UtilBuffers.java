package com.peircean.glusterfs;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class UtilBuffers {

	private static final int TEMP_BUF_POOL_SIZE = 3;
	// Per-thread soft cache of the last temporary direct buffer

	private static ThreadLocal<SoftReference<ByteBuffer>>[] bufferPool;

	static {
		bufferPool = new ThreadLocal[TEMP_BUF_POOL_SIZE];
		for (int i = 0; i < TEMP_BUF_POOL_SIZE; i++)
			bufferPool[i] = new ThreadLocal<>();
	}

	static ByteBuffer getTemporaryDirectBuffer(int size) {
		ByteBuffer buf = null;
		// Grab a buffer if available
		for (int i = 0; i < TEMP_BUF_POOL_SIZE; i++) {
			SoftReference<ByteBuffer> ref = bufferPool[i].get();
			if ((ref != null) && ((buf = ref.get()) != null) && (buf.capacity() >= size)) {
				buf.rewind();
				buf.limit(size);
				bufferPool[i].set(null);
				return buf;
			}
		}

		// Make a new one
		return ByteBuffer.allocate(size);
	}

	static void releaseTemporaryDirectBuffer(ByteBuffer buf) {
		if (buf == null)
			return;
		// Put it in an empty slot if such exists
		for (int i = 0; i < TEMP_BUF_POOL_SIZE; i++) {
			SoftReference<ByteBuffer> ref = (bufferPool[i].get());
			if ((ref == null) || (ref.get() == null)) {
				bufferPool[i].set(new SoftReference<>(buf));
				return;
			}
		}
		// Otherwise replace a smaller one in the cache if such exists
		for (int i = 0; i < TEMP_BUF_POOL_SIZE; i++) {
			SoftReference ref = (bufferPool[i].get());
			ByteBuffer inCacheBuf = (ByteBuffer) ref.get();
			if ((inCacheBuf == null) || (buf.capacity() > inCacheBuf.capacity())) {
				bufferPool[i].set(new SoftReference<>(buf));
				return;
			}
		}
	}

	static void erase(ByteBuffer bb) {
		byte[] inar = bb.array();
		for (int i = 0; i < inar.length; i++) {
			inar[i] = 0;
		}
	}

}
