package com.peircean.glusterfs;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;

public class GlusterWatchEvent implements WatchEvent<Path> {
	final private Path path;
	private Kind<Path> kind = StandardWatchEventKinds.ENTRY_CREATE;
	private int count = 0;
	private long lastModified;

	public GlusterWatchEvent(Path path) {
		super();
		this.path = path;
	}

	@Override
	public Kind<Path> kind() {
		return kind;
	}

	@Override
	public int count() {
		return count;
	}

	@Override
	public Path context() {
		return path;
	}

	Kind<Path> getKind() {
		return kind;
	}

	void setKind(Kind<Path> kind) {
		this.kind = kind;
	}

	int getCount() {
		return count;
	}

	void setCount(int count) {
		this.count = count;
	}

	long getLastModified() {
		return lastModified;
	}

	void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	Path getPath() {
		return path;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + count;
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + (int) (lastModified ^ (lastModified >>> 32));
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		GlusterWatchEvent other = (GlusterWatchEvent) obj;
		if (count != other.count)
			return false;
		if (kind == null) {
			if (other.kind != null)
				return false;
		} else if (!kind.equals(other.kind))
			return false;
		if (lastModified != other.lastModified)
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
}
