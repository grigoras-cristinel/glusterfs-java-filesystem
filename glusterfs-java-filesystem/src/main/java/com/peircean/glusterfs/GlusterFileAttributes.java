package com.peircean.glusterfs;

import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.peircean.libgfapi_jni.internal.structs.stat;

public class GlusterFileAttributes implements PosixFileAttributes {
	private static Map<Integer, PosixFilePermission> modeToPerms = new HashMap<>();
	private static Map<PosixFilePermission, Integer> permsToMode;

	static {
		modeToPerms.put(0001, PosixFilePermission.OTHERS_EXECUTE);
		modeToPerms.put(0002, PosixFilePermission.OTHERS_WRITE);
		modeToPerms.put(0004, PosixFilePermission.OTHERS_READ);
		modeToPerms.put(0010, PosixFilePermission.GROUP_EXECUTE);
		modeToPerms.put(0020, PosixFilePermission.GROUP_WRITE);
		modeToPerms.put(0040, PosixFilePermission.GROUP_READ);
		modeToPerms.put(0100, PosixFilePermission.OWNER_EXECUTE);
		modeToPerms.put(0200, PosixFilePermission.OWNER_WRITE);
		modeToPerms.put(0400, PosixFilePermission.OWNER_READ);

		permsToMode = invertModeMap(modeToPerms);
	}

	private final int mode, uid, gid;
	private final long size, atime, ctime, mtime, inode;

	public GlusterFileAttributes(int mode, int uid, int gid, long size, long atime, long ctime, long mtime,
			long inode) {
		super();
		this.mode = mode;
		this.uid = uid;
		this.gid = gid;
		this.size = size;
		this.atime = atime;
		this.ctime = ctime;
		this.mtime = mtime;
		this.inode = inode;
	}

	public static GlusterFileAttributes fromStat(stat stat) {
		return new GlusterFileAttributes(stat.st_mode, stat.st_uid, stat.st_gid, stat.st_size, stat.atime, stat.ctime,
				stat.mtime, stat.st_ino);
	}

	public static int parseAttrs(FileAttribute<?>... attrs) {
		int mode = 0;
		for (FileAttribute a : attrs) {
			for (PosixFilePermission p : (Set<PosixFilePermission>) a.value()) {

				Integer perm = permsToMode.get(p);

				if (null != perm) {
					mode |= perm;
				}
			}
		}
		return mode;
	}

	public static int permsToMode(Set<PosixFilePermission> attrs) {
		int mode = 0;

		for (PosixFilePermission p : attrs) {

			Integer perm = permsToMode.get(p);

			if (null != perm) {
				mode |= perm;
			}
		}

		return mode;
	}

	private static Map<PosixFilePermission, Integer> invertModeMap(Map<Integer, PosixFilePermission> modeToPerms) {

		HashMap<PosixFilePermission, Integer> permsToMode = new HashMap<>();

		for (Map.Entry<Integer, PosixFilePermission> entry : modeToPerms.entrySet()) {
			permsToMode.put(entry.getValue(), entry.getKey());
		}

		return permsToMode;
	}

	@Override
	public UserPrincipal owner() {
		return new UserPrincipal() {
			@Override
			public String getName() {
				return String.valueOf(uid);
			}
		};
	}

	@Override
	public GroupPrincipal group() {
		return new GroupPrincipal() {
			@Override
			public String getName() {
				return String.valueOf(gid);
			}
		};
	}

	@Override
	public Set<PosixFilePermission> permissions() {
		Set<PosixFilePermission> permissions = new HashSet<>();
		for (int mask : modeToPerms.keySet()) {
			if (mask == (mode & mask)) {
				permissions.add(modeToPerms.get(mask));
			}
		}
		return permissions;
	}

	@Override
	public FileTime lastModifiedTime() {
		return FileTime.fromMillis(mtime * 1000);
	}

	@Override
	public FileTime lastAccessTime() {
		return FileTime.fromMillis(atime * 1000);
	}

	@Override
	public FileTime creationTime() {
		return FileTime.fromMillis(ctime * 1000);
	}

	@Override
	public boolean isRegularFile() {
		int mask = 0100000;
		return mask == (mode & mask);
	}

	@Override
	public boolean isDirectory() {
		int mask = 0040000;
		return mask == (mode & mask);
	}

	@Override
	public boolean isSymbolicLink() {
		int mask = 0120000;
		return mask == (mode & mask);
	}

	@Override
	public boolean isOther() {
		return !(isDirectory() || isRegularFile() || isSymbolicLink());
	}

	@Override
	public long size() {
		return size;
	}

	@Override
	public Object fileKey() {
		return inode;
	}

	public static Map<Integer, PosixFilePermission> getModeToPerms() {
		return modeToPerms;
	}

	public static void setModeToPerms(Map<Integer, PosixFilePermission> modeToPerms) {
		GlusterFileAttributes.modeToPerms = modeToPerms;
	}

	public static Map<PosixFilePermission, Integer> getPermsToMode() {
		return permsToMode;
	}

	public static void setPermsToMode(Map<PosixFilePermission, Integer> permsToMode) {
		GlusterFileAttributes.permsToMode = permsToMode;
	}

	public int getMode() {
		return mode;
	}

	public int getUid() {
		return uid;
	}

	public int getGid() {
		return gid;
	}

	public long getSize() {
		return size;
	}

	public long getAtime() {
		return atime;
	}

	public long getCtime() {
		return ctime;
	}

	public long getMtime() {
		return mtime;
	}

	public long getInode() {
		return inode;
	}
}
