package com.peircean.glusterfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.GlusterOpenOption;
import com.peircean.libgfapi_jni.internal.UtilJNI;
import com.peircean.libgfapi_jni.internal.structs.stat;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class GlusterFileChannel extends FileChannel {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(GlusterFileChannel.class.getName());

	public static final Map<StandardOpenOption, Integer> optionMap = new HashMap<>();
	public static final Map<PosixFilePermission, Integer> perms = new HashMap<>();
	private static final int TRANSFER_SIZE = 8192;
	private static final int TRANSFER_SIZE_GLUSTER = 8192;

	static {
		optionMap.put(StandardOpenOption.APPEND, GlusterOpenOption.O_APPEND);
		optionMap.put(StandardOpenOption.CREATE, GlusterOpenOption.O_CREAT);
		optionMap.put(StandardOpenOption.CREATE_NEW, GlusterOpenOption.O_CREAT | GlusterOpenOption.O_EXCL);
		optionMap.put(StandardOpenOption.DSYNC, GlusterOpenOption.O_DSYNC);
		optionMap.put(StandardOpenOption.READ, GlusterOpenOption.O_RDONLY);
		optionMap.put(StandardOpenOption.WRITE, GlusterOpenOption.O_RDWR);
		optionMap.put(StandardOpenOption.TRUNCATE_EXISTING, GlusterOpenOption.O_TRUNC);

		perms.put(PosixFilePermission.OTHERS_EXECUTE, 0001);
		perms.put(PosixFilePermission.OTHERS_WRITE, 0002);
		perms.put(PosixFilePermission.OTHERS_READ, 0004);
		perms.put(PosixFilePermission.GROUP_EXECUTE, 0010);
		perms.put(PosixFilePermission.GROUP_WRITE, 0020);
		perms.put(PosixFilePermission.GROUP_READ, 0040);
		perms.put(PosixFilePermission.OWNER_EXECUTE, 0100);
		perms.put(PosixFilePermission.OWNER_WRITE, 0200);
		perms.put(PosixFilePermission.OWNER_READ, 0400);
	}

	private GlusterFileSystem fileSystem;

	private GlusterPath path;

	public GlusterPath getPath() {
		return path;
	}

	private Set<? extends OpenOption> options = new HashSet<>();

	public Set<? extends OpenOption> getOptions() {
		return options;
	}

	public void setOptions(Set<? extends OpenOption> options) {
		this.options = options;
	}

	public boolean isClosed() {
		return closed;
	}

	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	private FileAttribute<?> attrs[] = null;
	private long fileptr;
	private long position;

	public long getPosition() {
		return position;
	}

	public void setPosition(long position) {
		this.position = position;
	}

	private boolean closed = false;
	private boolean writable;

	public GlusterFileChannel() {
		super();
	}

	void init(GlusterFileSystem fileSystem, Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
			throws IOException {
		this.fileSystem = fileSystem;
		if (!path.isAbsolute()) {
			throw new IllegalStateException("Only absolute paths are supported at this time");
		}
		this.path = (GlusterPath) path;
		this.options = options;

		int flags = parseOptions(options);

		int mode = GlusterFileAttributes.parseAttrs(attrs);
		if (attrs == null || attrs.length == 0 || mode == 0) {
			/*
			 * Daca nu am fileAttributes fisierul este creat cu mode 0 ceea ce
			 * nu e bine. Pun default -rw-rw-rw-
			 */
			mode = 438;
		}
		String pathString = path.toUri().getPath();
		boolean createNew = options.contains(StandardOpenOption.CREATE_NEW);
		if (options.contains(StandardOpenOption.CREATE) || createNew) {
			fileptr = GLFS.glfs_creat(fileSystem.getVolptr(), pathString, flags, mode);
			writable = true;
		}

		if (createNew && 0 == fileptr) {
			throw new FileAlreadyExistsException(path.toString());
		}

		if (0 >= fileptr) {
			fileptr = GLFS.glfs_open(fileSystem.getVolptr(), pathString, flags);
			System.out.println(
					"Cred ca am dat open aici:" + fileSystem.getVolptr() + " flags :" + flags + " paths " + pathString);
			writable = true;
		}
		if (0 >= fileptr) {
			throw new IOException(
					"Unable to create or open file '" + pathString + "' on volume '" + fileSystem.toString() + "'");
		}
	}

	int parseOptions(Set<? extends OpenOption> options) {
		int opt = 0;
		for (OpenOption o : options) {
			if (!optionMap.containsKey(o)) {
				throw new UnsupportedOperationException("Option " + o + " is not supported at this time");
			}
			opt |= optionMap.get(o);
		}
		return opt;
	}

	@Override
	public int read(ByteBuffer byteBuffer) throws IOException {
		guardClosed();
		byte[] bytes = byteBuffer.array();
		long read = GLFS.glfs_read(fileptr, bytes, bytes.length, 0);
		if (read < 0) {
			throw new IOException(UtilJNI.strerror());
		}
		position += read;
		byteBuffer.position((int) read);
		if (0 == read) {
			/*
			 * End of file , precum in glfs-util.c
			 */
			return -1;
		}
		return (int) read;
	}

	@Override
	public long read(ByteBuffer[] byteBuffers, int offset, int length) throws IOException {
		guardClosed();
		if (length < 0 || length > byteBuffers.length - offset) {
			throw new IndexOutOfBoundsException("Length provided is invalid.");
		}
		if (offset < 0 || offset > byteBuffers.length) {
			throw new IndexOutOfBoundsException("Offset provided is invalid.");
		}
		if (!options.contains(StandardOpenOption.READ)) {
			throw new NonReadableChannelException();
		}

		long totalRead = 0L;
		try {
			totalRead = readHelper(byteBuffers, offset, length);
		} finally {
			if (totalRead > 0) {
				position += totalRead;
			}
		}
		return totalRead;
	}

	long readHelper(ByteBuffer[] byteBuffers, int offset, int length) throws IOException {
		long totalRead = 0L;
		boolean endOfStream = false;
		for (int i = offset; i < length + offset && !endOfStream; i++) {
			byte[] bytes = byteBuffers[i].array();
			int remaining;
			while ((remaining = byteBuffers[i].remaining()) > 0) {
				long read = GLFS.glfs_read(fileptr, bytes, remaining, 0);
				if (read < 0) {
					throw new IOException(UtilJNI.strerror());
				}
				totalRead += read;
				byteBuffers[i].position((int) read);
				if (0 == read) {
					endOfStream = true;
					break;
				}
			}
		}

		if (endOfStream && totalRead == 0) {
			return -1;
		}

		return totalRead;
	}

	@Override
	public int write(ByteBuffer byteBuffer) throws IOException {
		guardClosed();
		if (byteBuffer.hasArray()) {
			byte[] buf = byteBuffer.array();
			int written = GLFS.glfs_write(fileptr, buf, byteBuffer.remaining(), 0);
			if (written < 0) {
				throw new IOException(UtilJNI.strerror());
			}
			position += written;
			byteBuffer.position(written);
			return written;
		} else {
			throw new IOException(UtilJNI.strerror());
		}
	}

	@Override
	public long write(ByteBuffer[] byteBuffers, int offset, int length) throws IOException {
		guardClosed();
		if (offset < 0 || offset > byteBuffers.length) {
			throw new IndexOutOfBoundsException("Offset provided is invalid.");
		}
		if (length < 0 || length > byteBuffers.length - offset) {
			throw new IndexOutOfBoundsException("Length provided is invalid");
		}
		if (!options.contains(StandardOpenOption.WRITE)) {
			throw new NonWritableChannelException();
		}

		long totalWritten = 0L;

		for (int i = offset; i < length + offset; i++) {
			int remaining = byteBuffers[i].remaining();
			while (remaining > 0) {
				byte[] bytes = byteBuffers[i].array();
				int written = GLFS.glfs_write(fileptr, bytes, remaining, 0);
				if (written < 0) {
					throw new IOException();
				}
				position += written;
				byteBuffers[i].position(written);
				totalWritten += written;
				remaining = byteBuffers[i].remaining();
			}
		}
		return totalWritten;
	}

	@Override
	public long position() throws IOException {
		guardClosed();
		return position;
	}

	@Override
	public FileChannel position(long offset) throws IOException {
		guardClosed();
		if (offset < 0) {
			throw new IllegalArgumentException("offset can't be negative");
		}
		int whence = 0; // SEEK_SET
		int seek = GLFS.glfs_lseek(fileptr, offset, whence);
		position = offset;
		return this;
	}

	void guardClosed() throws ClosedChannelException {
		if (closed) {
			throw new ClosedChannelException();
		}
	}

	@Override
	public long size() throws IOException {
		stat stat = new stat();
		int retval = GLFS.glfs_fstat(fileptr, stat);
		if (0 != retval) {
			throw new IOException("fstat failed");
		}
		return stat.st_size;
	}

	@Override
	public FileChannel truncate(long l) throws IOException {
		return null; // To change body of implemented methods use File |
						// Settings | File Templates.
	}

	@Override
	public void force(boolean b) throws IOException {
		guardClosed();
		int fsync = GLFS.glfs_fsync(fileptr);
		if (0 != fsync) {
			throw new IOException("Unable to fsync");
		}
	}

	@Override
	public long transferTo(long position, long count, WritableByteChannel writableByteChannel) throws IOException {
		guardClosed();
		if (!this.isOpen())
			throw new ClosedChannelException();
		if ((position < 0) || (count < 0))
			throw new IllegalArgumentException();
		if (position > size())
			return 0;
		int icount = (int) Math.min(count, Integer.MAX_VALUE);
		long sz = size();
		if ((sz - position) < icount)
			icount = (int) (sz - position);
		if (writableByteChannel instanceof GlusterFileChannel) {
			System.out.println("Scriu in gluster.");
			return ((GlusterFileChannel) writableByteChannel).transferFromFileChannel(this, position, count);
		}
		return transferToArbitraryChannel(position, icount, writableByteChannel);
	}

	private long transferToArbitraryChannel(long position, int icount, WritableByteChannel target) throws IOException {
		int c = Math.min(icount, TRANSFER_SIZE);
		ByteBuffer bb = UtilBuffers.getTemporaryDirectBuffer(c);
		long tw = 0; // Total bytes written
		long pos = position;
		try {
			UtilBuffers.erase(bb);
			while (tw < icount) {
				bb.limit(Math.min((int) (icount - tw), TRANSFER_SIZE));
				int nr = read(bb, pos);
				if (nr <= 0)
					break;
				bb.flip();
				// ## Bug: Will block writing target if this channel
				// ## is asynchronously closed
				int nw = target.write(bb);
				tw += nw;
				if (nw != nr)
					break;
				pos += nw;
				bb.clear();
			}
			return tw;
		} catch (IOException x) {
			if (tw > 0)
				return tw;
			throw x;
		} finally {
			UtilBuffers.releaseTemporaryDirectBuffer(bb);
		}
	}

	@Override
	public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
		guardClosed();
		if (!src.isOpen())
			throw new ClosedChannelException();
		if (!writable)
			throw new NonWritableChannelException();
		if ((position < 0) || (count < 0))
			throw new IllegalArgumentException();
		if (position > size())
			return 0;
		if (src instanceof GlusterFileChannel)
			return transferFromFileChannel((GlusterFileChannel) src, position, count);
		return transferFromArbitraryChannel(src, position, count);
	}

	private long transferFromFileChannel(GlusterFileChannel src, long position2, long count) throws IOException {
		// Untrusted target: Use a newly-erased buffer
		int c = (int) Math.min(count, TRANSFER_SIZE_GLUSTER);
		ByteBuffer bb = UtilBuffers.getTemporaryDirectBuffer(c);
		long tw = 0; // Total bytes written
		long pos = position;
		try {
			UtilBuffers.erase(bb);
			while (tw < count) {
				bb.limit((int) Math.min((count - tw), TRANSFER_SIZE_GLUSTER));
				// ## Bug: Will block reading src if this channel
				// ## is asynchronously closed
				int nr = src.read(bb);
				if (nr <= 0)
					break;
				bb.flip();
				int nw = write(bb, pos);
				tw += nw;
				if (nw != nr)
					break;
				pos += nw;
				bb.clear();
			}
			if (tw > count) {
				throw new RuntimeException("Exceptie transfer lungime gresita");
			}
			return tw;
		} catch (IOException x) {
			if (tw > 0)
				return tw;
			throw x;
		} finally {
			UtilBuffers.releaseTemporaryDirectBuffer(bb);
		}
	}

	private long transferFromArbitraryChannel(ReadableByteChannel src, long position, long count) throws IOException {
		// Untrusted target: Use a newly-erased buffer
		int c = (int) Math.min(count, TRANSFER_SIZE);
		ByteBuffer bb = UtilBuffers.getTemporaryDirectBuffer(c);
		long tw = 0; // Total bytes written
		long pos = position;
		try {
			UtilBuffers.erase(bb);
			while (tw < count) {
				bb.limit((int) Math.min((count - tw), TRANSFER_SIZE));
				// ## Bug: Will block reading src if this channel
				// ## is asynchronously closed
				int nr = src.read(bb);
				if (nr <= 0)
					break;
				bb.flip();
				int nw = writeAtPositionNoCheck(bb, pos);
				tw += nw;
				if (nw != nr)
					break;
				pos += nw;
				bb.clear();
			}
			return tw;
		} catch (IOException x) {
			if (tw > 0)
				return tw;
			throw x;
		} finally {
			UtilBuffers.releaseTemporaryDirectBuffer(bb);
		}

	}

	@Override
	public int read(ByteBuffer byteBuffer, long position) throws IOException {
		guardClosed();
		if (position < 0) {
			throw new IllegalArgumentException();
		}
		if (!options.contains(StandardOpenOption.READ)) {
			throw new NonReadableChannelException();
		}
		if (position >= size()) {
			return -1;
		}
		int whence = 0; // SEEK_SET
		int seek = GLFS.glfs_lseek(fileptr, position, whence);
		if (seek < 0) {
			throw new IOException();
		}
		byte[] bytes = byteBuffer.array();
		long read = GLFS.glfs_read(fileptr, bytes, bytes.length, 0);

		if (0 > read) {
			throw new IOException();
		}
		byteBuffer.position((int) read);
		seek = GLFS.glfs_lseek(fileptr, this.position, whence);
		if (0 > seek) {
			throw new IOException(UtilJNI.strerror());
		}
		return (int) read;
	}

	@Override
	public int write(ByteBuffer byteBuffer, long position) throws IOException {
		if (byteBuffer == null) {
			throw new NullPointerException();
		}
		if (position < 0) {
			throw new IllegalArgumentException("Negative position");
		}
		guardClosed();
		if (!options.contains(StandardOpenOption.WRITE)) {
			throw new NonWritableChannelException();
		}
		return writeAtPositionNoCheck(byteBuffer, position);
	}

	private int writeAtPositionNoCheck(ByteBuffer byteBuffer, long position) throws IOException {
		try {
			int bytesToWrite = byteBuffer.remaining();
			long lsize = size();
			if (position >= lsize) {
				/*
				 * If has to write ouside off seek
				 */
				byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), 0, bytesToWrite);
				byte[] temp = Arrays.copyOf(bytes, bytes.length + (int) (position - lsize));
				bytesToWrite = temp.length;
				byteBuffer = ByteBuffer.wrap(temp);
			}
			int whence = 0; // SEEK_SET
			int seek = GLFS.glfs_lseek(fileptr, position, whence);
			if (seek < 0) {
				throw new IOException();
			}
			byte[] bytes = byteBuffer.array();
			long written = GLFS.glfs_write(fileptr, bytes, bytes.length, 0);
			seek = GLFS.glfs_lseek(fileptr, this.position, whence);
			if (seek < 0) {
				throw new IOException();
			}
			byteBuffer.position((int) written);
			return (int) written;
		} finally {

		}
	}

	@Override
	public MappedByteBuffer map(MapMode mapMode, long l, long l2) throws IOException {
		throw new IOException("Unsuported operation exception");
	}

	@Override
	public FileLock lock(long l, long l2, boolean b) throws IOException {
		throw new IOException("Unsuported operation exception");
	}

	@Override
	public FileLock tryLock(long l, long l2, boolean b) throws IOException {
		throw new IOException("Unsuported operation exception");
	}

	@Override
	protected void implCloseChannel() throws IOException {
		if (!closed) {
			int close = GLFS.glfs_close(fileptr);
			if (0 != close) {
				throw new IOException("Close returned nonzero");
			}
			closed = true;
		}
	}

	public FileAttribute<?>[] getAttrs() {
		return attrs;
	}

	public void setAttrs(FileAttribute<?>[] attrs) {
		this.attrs = attrs;
	}

	public long getFileptr() {
		return fileptr;
	}

	public void setFileptr(long fileptr) {
		this.fileptr = fileptr;
	}

	public GlusterFileSystem getFileSystem() {
		return fileSystem;
	}

}
