package com.peircean.glusterfs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GlusterWatchKey implements WatchKey {
	private boolean valid = true;
	private boolean ready = true;
	Map<Path, GlusterWatchEvent> events = new HashMap<>();
	final private GlusterPath path;
	private WatchEvent.Kind[] kinds;
	private long lastPolled = (new Date()).getTime();

	public GlusterWatchKey(GlusterPath path, Kind[] kinds) {
		super();
		this.path = path;
		this.kinds = kinds;
	}

	public GlusterWatchKey(GlusterPath path) {
		super();
		this.path = path;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	public boolean update() {
		DirectoryStream<Path> paths;
		try {
			paths = Files.newDirectoryStream(path);
		} catch (IOException e) {
			return false;
		}
		List<Path> files = new LinkedList<>();
		boolean newEvents = false;
		for (Path f : paths) {
			newEvents |= processExistingFile(files, f);
		}
		for (Path f : events.keySet()) {
			newEvents |= checkDeleted(files, f);
		}
		return newEvents;
	}

	boolean processExistingFile(List<Path> files, Path f) {
		if (Files.isDirectory(f)) {
			return false;
		}
		files.add(f);

		long lastModified;
		try {
			lastModified = Files.getLastModifiedTime(f).toMillis();
		} catch (IOException e) {
			return false;
		}

		GlusterWatchEvent event = events.get(f);
		if (null != event) {
			return checkModified(event, lastModified);
		} else {
			return checkCreated(f, lastModified);
		}
	}

	boolean checkDeleted(List<Path> files, Path f) {
		GlusterWatchEvent event = events.get(f);
		if (!files.contains(f) && !StandardWatchEventKinds.ENTRY_DELETE.name().equals(event.kind().name())) {
			event.setLastModified((new Date()).getTime());
			event.setKind(StandardWatchEventKinds.ENTRY_DELETE);
			event.setCount(event.getCount() + 1);
			return true;
		}
		return false;
	}

	boolean checkCreated(Path f, long lastModified) {
		GlusterWatchEvent event = new GlusterWatchEvent(f.getFileName());
		event.setLastModified(lastModified);
		events.put(f, event);
		return (lastModified > lastPolled);
	}

	boolean checkModified(GlusterWatchEvent event, long lastModified) {
		if (lastModified > event.getLastModified()) {
			event.setLastModified(lastModified);
			if (event.kind().name().equals(StandardWatchEventKinds.ENTRY_DELETE.name())) {
				event.setKind(StandardWatchEventKinds.ENTRY_CREATE);
				event.setCount(0);
			} else {
				event.setKind(StandardWatchEventKinds.ENTRY_MODIFY);
				event.setCount(event.getCount() + 1);
			}
			return true;
		}
		return false;
	}

	boolean kindsContains(WatchEvent.Kind kind) {
		for (WatchEvent.Kind k : kinds) {
			if (k.name().equals(kind.name())) {
				return true;
			}
		}
		return false;
	}

	@Override
	synchronized public List<WatchEvent<?>> pollEvents() {
		if (!ready) {
			return new LinkedList<>();
		}
		ready = false;
		return findPendingEvents();
	}

	LinkedList<WatchEvent<?>> findPendingEvents() {
		long maxModifiedTime = lastPolled;
		LinkedList<WatchEvent<?>> pendingEvents = new LinkedList<>();
		for (Path p : events.keySet()) {
			long lastModified = queueEventIfPending(pendingEvents, p);
			maxModifiedTime = Math.max(maxModifiedTime, lastModified);
		}
		lastPolled = maxModifiedTime;
		return pendingEvents;
	}

	private long queueEventIfPending(LinkedList<WatchEvent<?>> pendingEvents, Path p) {
		GlusterWatchEvent e = events.get(p);
		long lastModified = e.getLastModified();
		if (lastModified > lastPolled && kindsContains(e.kind())) {
			pendingEvents.add(e);
		}
		return lastModified;
	}

	@Override
	synchronized public boolean reset() {
		if (!valid || ready) {
			return false;
		} else {
			ready = true;
			return true;
		}
	}

	@Override
	public void cancel() {
		valid = false;
	}

	@Override
	public Watchable watchable() {
		return path;
	}

	boolean isReady() {
		return ready;
	}

	void setReady(boolean ready) {
		this.ready = ready;
	}

	Map<Path, GlusterWatchEvent> getEvents() {
		return events;
	}

	void setEvents(Map<Path, GlusterWatchEvent> events) {
		this.events = events;
	}

	WatchEvent.Kind[] getKinds() {
		return kinds;
	}

	void setKinds(WatchEvent.Kind[] kinds) {
		this.kinds = kinds;
	}

	long getLastPolled() {
		return lastPolled;
	}

	void setLastPolled(long lastPolled) {
		this.lastPolled = lastPolled;
	}

	GlusterPath getPath() {
		return path;
	}

	void setValid(boolean valid) {
		this.valid = valid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (lastPolled ^ (lastPolled >>> 32));
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + (ready ? 1231 : 1237);
		result = prime * result + (valid ? 1231 : 1237);
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
		GlusterWatchKey other = (GlusterWatchKey) obj;
		if (lastPolled != other.lastPolled)
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (ready != other.ready)
			return false;
		if (valid != other.valid)
			return false;
		return true;
	}
}
