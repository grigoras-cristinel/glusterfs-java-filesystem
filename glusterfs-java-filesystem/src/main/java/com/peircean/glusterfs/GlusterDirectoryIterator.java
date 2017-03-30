package com.peircean.glusterfs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.structs.dirent;

class GlusterDirectoryIterator<T> implements Iterator<GlusterPath> {
	private GlusterDirectoryStream stream;
	private DirectoryStream.Filter<? super Path> filter;
	private dirent current, next;
	private GlusterPath nextPath;

	@Override
	public boolean hasNext() {
		advance();
		if (null != filter) {
			try {
				while (next.d_ino != 0 && !filter.accept(nextPath)) {
					advance();
				}
			} catch (IOException e) {
				current = null;
				next = null;
				return false;
			}
		}

		if (next != null && next.d_ino == 0) {
			current = null;
			next = null;
			return false;
		}

		return true;
	}

	void advance() {
		String name;
		do {
			current = new dirent();
			long nextPtr = dirent.malloc(dirent.SIZE_OF);
			GLFS.glfs_readdir_r(stream.getDirHandle(), current, nextPtr);

			next = new dirent();
			dirent.memmove(next, nextPtr, dirent.SIZE_OF);
			dirent.free(nextPtr);

			name = current.getName();
			nextPath = (GlusterPath) stream.getDir().resolve(name);
		} while (name.equals(".") || name.equals(".."));
	}

	@Override
	public GlusterPath next() {
		if (nextPath == null) {
			throw new NoSuchElementException("No more entries");
		}

		return nextPath;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public GlusterDirectoryStream getStream() {
		return stream;
	}

	public void setStream(GlusterDirectoryStream stream) {
		this.stream = stream;
	}

	public DirectoryStream.Filter<? super Path> getFilter() {
		return filter;
	}

	public void setFilter(DirectoryStream.Filter<? super Path> filter) {
		this.filter = filter;
	}

	public dirent getCurrent() {
		return current;
	}

	public void setCurrent(dirent current) {
		this.current = current;
	}

	public dirent getNext() {
		return next;
	}

	public void setNext(dirent next) {
		this.next = next;
	}

	public GlusterPath getNextPath() {
		return nextPath;
	}

	public void setNextPath(GlusterPath nextPath) {
		this.nextPath = nextPath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((current == null) ? 0 : current.hashCode());
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + ((next == null) ? 0 : next.hashCode());
		result = prime * result + ((nextPath == null) ? 0 : nextPath.hashCode());
		result = prime * result + ((stream == null) ? 0 : stream.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GlusterDirectoryIterator other = (GlusterDirectoryIterator) obj;
		if (current == null) {
			if (other.current != null)
				return false;
		} else if (!current.equals(other.current))
			return false;
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (next == null) {
			if (other.next != null)
				return false;
		} else if (!next.equals(other.next))
			return false;
		if (nextPath == null) {
			if (other.nextPath != null)
				return false;
		} else if (!nextPath.equals(other.nextPath))
			return false;
		if (stream == null) {
			if (other.stream != null)
				return false;
		} else if (!stream.equals(other.stream))
			return false;
		return true;
	}
}
