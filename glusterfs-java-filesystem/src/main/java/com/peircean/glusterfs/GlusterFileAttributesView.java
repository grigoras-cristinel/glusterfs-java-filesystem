package com.peircean.glusterfs;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.peircean.libgfapi_jni.internal.GLFS;
import com.peircean.libgfapi_jni.internal.structs.timespec;

public class GlusterFileAttributesView {

	private static final class PosixFileAttributeViewImplementation implements PosixFileAttributeView {
		private final LinkOption[] linkOptions;
		private final GlusterPath file;
		private GlusterFileAttributes readAttributes;

		public PosixFileAttributeViewImplementation(GlusterPath file, LinkOption[] linkOptions) throws IOException {
			this.linkOptions = linkOptions;
			this.file = file;
			readAttributes = file.getFileSystem().getProvider().readAttributes(file, GlusterFileAttributes.class,
					linkOptions);
		}

		@Override
		public void setOwner(UserPrincipal owner) throws IOException {
			// TODO Auto-generated method stub
			// ignored
		}

		@Override
		public UserPrincipal getOwner() throws IOException {
			return readAttributes.owner();
		}

		@Override
		public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
				throws IOException {
			timespec[] times = new timespec[2];
			times[0] = new timespec();
			times[0].tv_sec = lastAccessTime.to(TimeUnit.SECONDS);
			times[0].tv_nsec = lastAccessTime.to(TimeUnit.NANOSECONDS);
			times[1] = new timespec();
			times[1].tv_sec = lastModifiedTime.to(TimeUnit.SECONDS);
			times[1].tv_nsec = lastModifiedTime.to(TimeUnit.NANOSECONDS);
			GLFS.glfs_utimens(file.getFileSystem().getVolptr(), file.toAbsolutePath().toString(), times);

		}

		@Override
		public void setPermissions(Set<PosixFilePermission> perms) throws IOException {
			int mode = GlusterFileAttributes.permsToMode(perms);
			GLFS.glfs_chmod(file.getFileSystem().getVolptr(), file.getString(), mode);
		}

		@Override
		public void setGroup(GroupPrincipal group) throws IOException {
			// ignored
		}

		@Override
		public PosixFileAttributes readAttributes() throws IOException {
			return readAttributes;
		}

		@Override
		public String name() {
			return "posix";
		}
	}

	private static final class BasicFileAttributeViewImplementation implements BasicFileAttributeView {
		private final LinkOption[] linkOptions;
		private final GlusterPath file;
		private GlusterFileAttributes readAttributes;

		private BasicFileAttributeViewImplementation(GlusterPath file, LinkOption[] linkOptions) throws IOException {
			this.linkOptions = linkOptions;
			this.file = file;
			readAttributes = file.getFileSystem().getProvider().readAttributes(file, GlusterFileAttributes.class,
					linkOptions);
		}

		@Override
		public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
				throws IOException {

		}

		@Override
		public BasicFileAttributes readAttributes() throws IOException {

			BasicFileAttributes retval1 = new BasicFileAttributes() {

				@Override
				public long size() {
					return readAttributes.size();
				}

				@Override
				public FileTime lastModifiedTime() {
					return FileTime.from(readAttributes.getMtime(), TimeUnit.SECONDS);
				}

				@Override
				public FileTime lastAccessTime() {
					return FileTime.from(readAttributes.getAtime(), TimeUnit.SECONDS);
				}

				@Override
				public boolean isSymbolicLink() {
					return readAttributes.isSymbolicLink();
				}

				@Override
				public boolean isRegularFile() {
					return readAttributes.isRegularFile();
				}

				@Override
				public boolean isOther() {
					return readAttributes.isOther();
				}

				@Override
				public boolean isDirectory() {
					return readAttributes.isDirectory();
				}

				@Override
				public Object fileKey() {
					return readAttributes.fileKey();
				}

				@Override
				public FileTime creationTime() {
					return FileTime.from(readAttributes.getCtime(), TimeUnit.SECONDS);
				}
			};
			return retval1;
		}

		@Override
		public String name() {
			return "basic";
		}
	}

	public static BasicFileAttributeView createBasicView(final GlusterPath file, final LinkOption[] linkOptions) {
		BasicFileAttributeView retval = null;
		try {
			retval = new BasicFileAttributeViewImplementation(file, linkOptions);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return retval;
	}

	public static PosixFileAttributeView createPosixView(GlusterPath file, LinkOption[] linkOptions) {
		PosixFileAttributeView retval = null;
		try {
			retval = new PosixFileAttributeViewImplementation(file, linkOptions);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return retval;
	}

	public static FileOwnerAttributeView createOwnerView(GlusterPath file, LinkOption[] linkOptions) {
		// TODO Auto-generated method stub
		return null;
	}
}
