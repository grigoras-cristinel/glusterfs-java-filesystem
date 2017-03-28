package com.peircean.glusterfs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class GlusterFileStore extends FileStore {
	public static final String GLUSTERFS = "glusterfs";
	private GlusterFileSystem fileSystem;

	public GlusterFileStore(GlusterFileSystem fileSystem) {
		super();
		this.fileSystem = fileSystem;
	}

	@Override
	public String name() {
		return fileSystem.getVolname();
	}

	@Override
	public String type() {
		return GLUSTERFS;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public long getTotalSpace() throws IOException {
		GlusterFileSystemProvider provider = (GlusterFileSystemProvider) fileSystem.provider();
		return provider.getTotalSpace(fileSystem.getVolptr());
	}

	@Override
	public long getUsableSpace() throws IOException {
		GlusterFileSystemProvider provider = (GlusterFileSystemProvider) fileSystem.provider();
		return provider.getUsableSpace(fileSystem.getVolptr());
	}

	@Override
	public long getUnallocatedSpace() throws IOException {
		GlusterFileSystemProvider provider = (GlusterFileSystemProvider) fileSystem.provider();
		return provider.getUnallocatedSpace(fileSystem.getVolptr());
	}

	@Override
	public boolean supportsFileAttributeView(Class<? extends FileAttributeView> aClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean supportsFileAttributeView(String s) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> vClass) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object getAttribute(String s) throws IOException {
		throw new UnsupportedOperationException();
	}

	GlusterFileSystem getFileSystem() {
		return fileSystem;
	}

	void setFileSystem(GlusterFileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileSystem == null) ? 0 : fileSystem.hashCode());
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
		GlusterFileStore other = (GlusterFileStore) obj;
		if (fileSystem == null) {
			if (other.fileSystem != null)
				return false;
		} else if (!fileSystem.equals(other.fileSystem))
			return false;
		return true;
	}
}
