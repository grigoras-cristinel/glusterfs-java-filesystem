package com.peircean.glusterfs;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class GlusterWatchService implements WatchService {
	public static final int MILLIS_PER_SECOND = 1000;
	public static final int MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
	public static final int MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;
	public static final int MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;
	public static long PERIOD = 100L;

	private Set<GlusterWatchKey> paths = new HashSet<>();
	private Set<GlusterWatchKey> pendingPaths = new HashSet<>();
	private boolean running = true;

	public WatchKey registerPath(GlusterPath path, WatchEvent.Kind... kinds) {
		if (!running) {
			throw new ClosedWatchServiceException();
		}
		for (GlusterWatchKey k : paths) {
			if (k.getPath().equals(path)) {
				k.setKinds(kinds);
				return k;
			}
		}
		GlusterWatchKey key = new GlusterWatchKey(path, kinds);
		paths.add(key);
		return key;
	}

	@Override
	public void close() throws IOException {
		if (running) {
			running = false;
			for (GlusterWatchKey k : paths) {
				k.cancel();
			}
		}
	}

	WatchKey popPending() {
		Iterator<GlusterWatchKey> iterator = pendingPaths.iterator();
		try {
			GlusterWatchKey key = iterator.next();
			iterator.remove();
			return key;
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public WatchKey poll() {
		if (!running) {
			throw new ClosedWatchServiceException();
		}
		WatchKey pending = popPending();
		if (null != pending) {
			return pending;
		}
		for (GlusterWatchKey k : paths) {
			if (k.isValid() && k.isReady() && k.update()) {
				pendingPaths.add(k);
			}
		}
		return popPending();
	}

	@Override
	public WatchKey poll(long timeout, TimeUnit unit) {
		long timeoutMillis = timeoutToMillis(timeout, unit);
		long loops = 0;
		while (running) {
			WatchKey key = poll();
			if (key != null) {
				return key;
			}
			if ((loops * PERIOD) > timeoutMillis) {
				return null;
			}
			loops++;
			try {
				Thread.sleep(PERIOD);
			} catch (InterruptedException e) {
			}
		}
		throw new ClosedWatchServiceException();
	}

	@Override
	public WatchKey take() {
		while (running) {
			WatchKey key = poll();
			if (key != null) {
				return key;
			}
			try {
				Thread.sleep(PERIOD);
			} catch (InterruptedException e) {
			}
		}
		throw new ClosedWatchServiceException();
	}

	long timeoutToMillis(long timeout, TimeUnit unit) {
		switch (unit) {
		case DAYS:
			return (timeout * MILLIS_PER_DAY);
		case HOURS:
			return (timeout * MILLIS_PER_HOUR);
		case MINUTES:
			return (timeout * MILLIS_PER_MINUTE);
		case SECONDS:
			return (timeout * MILLIS_PER_SECOND);
		case MILLISECONDS:
			return timeout;
		default: // MICROS & NANOS
			return -1;
		}
	}

	Set<GlusterWatchKey> getPaths() {
		return paths;
	}

	void setPaths(Set<GlusterWatchKey> paths) {
		this.paths = paths;
	}

	Set<GlusterWatchKey> getPendingPaths() {
		return pendingPaths;
	}

	void setPendingPaths(Set<GlusterWatchKey> pendingPaths) {
		this.pendingPaths = pendingPaths;
	}

	boolean isRunning() {
		return running;
	}

	void setRunning(boolean running) {
		this.running = running;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((paths == null) ? 0 : paths.hashCode());
		result = prime * result + (running ? 1231 : 1237);
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
		GlusterWatchService other = (GlusterWatchService) obj;
		if (paths == null) {
			if (other.paths != null)
				return false;
		} else if (!paths.equals(other.paths))
			return false;
		if (running != other.running)
			return false;
		return true;
	}

}
