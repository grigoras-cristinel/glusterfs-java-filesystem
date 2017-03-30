package com.peircean.glusterfs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

import com.peircean.libgfapi_jni.internal.GLFS;

public class GlusterDirectoryStream implements DirectoryStream<Path> {
	private GlusterFileSystem fileSystem;
	private long dirHandle = 0;
	private GlusterDirectoryIterator iterator;
	private boolean closed = false;
	private GlusterPath dir;
	private DirectoryStream.Filter<? super Path> filter;

	@Override
	public Iterator<Path> iterator() {
		if (null != iterator || closed) {
			throw new IllegalStateException("Already iterating!");
		}
		GlusterDirectoryIterator iterator = new GlusterDirectoryIterator();
		iterator.setStream(this);
		iterator.setFilter(filter);
		this.iterator = iterator;
		return iterator;
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			GLFS.glfs_close(dirHandle);
			closed = true;
		}
	}

	public void open(GlusterPath path) {
		dir = path;
		if (dirHandle == 0) {
			dirHandle = GLFS.glfs_opendir(path.getFileSystem().getVolptr(), path.getString());
		} else {
			throw new IllegalStateException("Already open!");
		}
	}

	public GlusterFileSystem getFileSystem() {
		return fileSystem;
	}

	public void setFileSystem(GlusterFileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	public long getDirHandle() {
		return dirHandle;
	}

	public void setDirHandle(long dirHandle) {
		this.dirHandle = dirHandle;
	}

	public GlusterDirectoryIterator getIterator() {
		return iterator;
	}

	public void setIterator(GlusterDirectoryIterator iterator) {
		this.iterator = iterator;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	public GlusterPath getDir() {
		return dir;
	}

	public void setDir(GlusterPath dir) {
		this.dir = dir;
	}

	public DirectoryStream.Filter<? super Path> getFilter() {
		return filter;
	}

	public void setFilter(DirectoryStream.Filter<? super Path> filter) {
		this.filter = filter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (closed ? 1231 : 1237);
		result = prime * result + ((dir == null) ? 0 : dir.hashCode());
		result = prime * result + (int) (dirHandle ^ (dirHandle >>> 32));
		result = prime * result + ((fileSystem == null) ? 0 : fileSystem.hashCode());
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + ((iterator == null) ? 0 : iterator.hashCode());
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
		GlusterDirectoryStream other = (GlusterDirectoryStream) obj;
		if (closed != other.closed)
			return false;
		if (dir == null) {
			if (other.dir != null)
				return false;
		} else if (!dir.equals(other.dir))
			return false;
		if (dirHandle != other.dirHandle)
			return false;
		if (fileSystem == null) {
			if (other.fileSystem != null)
				return false;
		} else if (!fileSystem.equals(other.fileSystem))
			return false;
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (iterator == null) {
			if (other.iterator != null)
				return false;
		} else if (!iterator.equals(other.iterator))
			return false;
		return true;
	}
}
