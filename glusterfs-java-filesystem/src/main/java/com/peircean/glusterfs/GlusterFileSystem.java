package com.peircean.glusterfs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.peircean.glusterfs.borrowed.GlobPattern;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */

public class GlusterFileSystem extends FileSystem {
	private static final String SEPARATOR = "/";
	private final GlusterFileSystemProvider provider;
	private final String host;
	private final String volname;

	public GlusterFileSystem(GlusterFileSystemProvider provider, String host, String volname, long volptr) {
		super();
		this.provider = provider;
		this.host = host;
		this.volname = volname;
		this.volptr = volptr;
	}

	private long volptr;

	@Override
	public FileSystemProvider provider() {
		return provider;
	}

	@Override
	public void close() throws IOException {

		if (isOpen()) {
			volptr = -1;
			int fini = provider.close(volptr);
			if (0 != fini) {
				throw new IOException("Unable to close filesystem: " + volname);
			}
		}
	}

	@Override
	public boolean isOpen() {
		return volptr > 0;
	}

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public String getSeparator() {
		return SEPARATOR;
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		GlusterPath root = new GlusterPath(this, "/");
		List<Path> list = new ArrayList<>(1);
		list.add(root);
		return Collections.unmodifiableList(list);
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		GlusterFileStore store = new GlusterFileStore(this);
		List<FileStore> stores = new ArrayList<>(1);
		stores.add(store);
		return Collections.unmodifiableList(stores);
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Path getPath(String s, String... strings) {
		boolean absolute = s.startsWith("/");
		if (absolute) {
			s = s.substring(1);
		}
		String[] parts;
		if (null != strings && strings.length > 0) {
			parts = new String[1 + strings.length];
			parts[0] = s;
			System.arraycopy(strings, 0, parts, 1, strings.length);
		} else {
			parts = new String[] { s };
		}
		return new GlusterPath(this, parts, absolute);
	}

	@Override
	public PathMatcher getPathMatcher(String s) {
		if (!s.contains(":")) {
			throw new IllegalArgumentException("PathMatcher requires input syntax:expression");
		}
		String[] parts = s.split(":", 2);
		Pattern pattern;
		if ("glob".equals(parts[0])) {
			pattern = GlobPattern.compile(parts[1]);
		} else if ("regex".equals(parts[0])) {
			pattern = Pattern.compile(parts[1]);
		} else {
			throw new UnsupportedOperationException("Unknown PathMatcher syntax: " + parts[0]);
		}

		return new GlusterPathMatcher(pattern);
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchService newWatchService() throws IOException {
		return new GlusterWatchService();
	}

	@Override
	public String toString() {
		return provider.getScheme() + "://" + host + ":" + volname;
	}

	long getVolptr() {
		return volptr;
	}

	GlusterFileSystemProvider getProvider() {
		return provider;
	}

	String getHost() {
		return host;
	}

	String getVolname() {
		return volname;
	}

	void setVolptr(long volptr) {
		this.volptr = volptr;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + ((volname == null) ? 0 : volname.hashCode());
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
		GlusterFileSystem other = (GlusterFileSystem) obj;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (volname == null) {
			if (other.volname != null)
				return false;
		} else if (!volname.equals(other.volname))
			return false;
		return true;
	}
}
