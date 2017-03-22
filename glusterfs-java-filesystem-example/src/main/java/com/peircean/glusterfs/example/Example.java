package com.peircean.glusterfs.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.spi.FileSystemProvider;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.peircean.glusterfs.GlusterFileSystem;
import com.peircean.glusterfs.GlusterPath;

/**
 * @author <a href="http://about.me/louiszuckerman">Louis Zuckerman</a>
 */
public class Example {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(Example.class.getName());
	private FileSystem fileSystem;
	private String serverUri;
	private FileSystemProvider fileSystemProvider;

	private long testUnic = Calendar.getInstance().getTimeInMillis();

	public Example() {
		super();
	}

	public FileSystemProvider getProvider(String scheme) {
		for (FileSystemProvider fsp : FileSystemProvider.installedProviders()) {
			if (fsp.getScheme().equals(scheme)) {
				return fsp;
			}
		}
		throw new IllegalArgumentException("No provider found for scheme: " + scheme);
	}

	public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
		logger.info("Start main program.");
		Example totclass = new Example();
		totclass.createFileSystem();
		totclass.readFileSystemInfo();
		totclass.testHiddenFiles();
		totclass.testPathCreation();
		totclass.testFileCreationPosixA();
		totclass.runToate();
		logger.info("End main program.");
	}

	private void createFileSystem() throws IOException, URISyntaxException {
		if (logger.isLoggable(Level.CONFIG)) {
			logger.logp(Level.CONFIG, "Example", "createFileSystem()", "start"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		Properties properties = new Properties();
		properties.load(Example.class.getClassLoader().getResourceAsStream("example.properties"));
		String server = properties.getProperty("glusterfs.server");
		String volname = properties.getProperty("glusterfs.volume");
		logger.info("Provider class is: " + getProvider("gluster").toString());
		serverUri = "gluster://" + server + ":" + volname + "/";
		fileSystem = FileSystems.newFileSystem(new URI(serverUri), null);
		fileSystemProvider = fileSystem.provider();

		if (logger.isLoggable(Level.CONFIG)) {
			logger.logp(Level.CONFIG, "Example", "createFileSystem()", "end"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	private void readFileSystemInfo() throws IOException {
		FileStore store = fileSystem.getFileStores().iterator().next();
		logger.logp(Level.INFO, "Example", "readFileSystemInfo()", "TOTAL SPACE: " + store.getTotalSpace()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.logp(Level.INFO, "Example", "readFileSystemInfo()", "USABLE SPACE: " + store.getUsableSpace()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		logger.logp(Level.INFO, "Example", "readFileSystemInfo()", //$NON-NLS-1$ //$NON-NLS-2$
				"UNALLOCATED SPACE: " + store.getUnallocatedSpace()); //$NON-NLS-1$
		logger.logp(Level.INFO, "Example", "readFileSystemInfo()", fileSystem.toString()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private void testHiddenFiles() throws IOException {
		String hidden = "/foo/.bar";
		boolean isHidden = fileSystemProvider.isHidden(new GlusterPath(((GlusterFileSystem) fileSystem), hidden));
		System.out.println("Is " + hidden + " hidden ? " + isHidden);
		hidden = "/foo/bar";
		isHidden = fileSystemProvider.isHidden(new GlusterPath(((GlusterFileSystem) fileSystem), hidden));
		System.out.println("Is " + hidden + " hidden ? " + isHidden);
	}

	private void testPathCreation() throws URISyntaxException {
		String testFile = serverUri + "baz" + testUnic;
		logger.info("Path string is: " + testFile);
		Path glusterPath = Paths.get(new URI(testFile));
		logger.info("Path from Paths is: " + glusterPath.getClass());
		logger.info("Path is:" + glusterPath);
		Path path = fileSystem.getPath("bazA" + testUnic);
		logger.info("Path from filesystem: " + path.toFile().toString());
		logger.info("File exists ? " + path + " - " + path.toFile().exists());
	}

	private void testFileCreationPosixA() throws URISyntaxException, IOException {
		Set<PosixFilePermission> posixFilePermissions = PosixFilePermissions.fromString("rw-rw-rw-");
		FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(posixFilePermissions);
		String testFile = serverUri + "baz" + testUnic;
		Path glusterPath = Paths.get(new URI(testFile));
		Path crf = Files.createFile(glusterPath, attrs);
		logger.info("Create file from Paths " + crf);
		Path path = fileSystem.getPath("/bazA" + testUnic);
		logger.info("Path from filesystem: " + path.toFile().toString());
		Path crf1 = Files.createFile(path, attrs);
		logger.info("File exists ? " + Files.exists(crf1));
	}

	private void runToate() throws IOException, URISyntaxException, InterruptedException {
		String testFile = serverUri + "bazB" + testUnic;

		String bigFileUri = serverUri + "bigfile" + testUnic + ".txt";
		Path bigFile = Paths.get(new URI(bigFileUri));
		if (Files.exists(bigFile)) {
			Files.delete(bigFile);
		}
		// Input stream to gluster
		logger.fine("TEST1 - Copy big file to gluster");
		InputStream res = Example.class.getResourceAsStream("/bigfile.txt");
		if (res != null) {
			Set<PosixFilePermission> posixFilePermissions = PosixFilePermissions.fromString("rw-rw-rw-");
			FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(posixFilePermissions);
			// FileChannel glusterBigFileChannel = FileChannel.open(createdFile,
			// StandardOpenOption.WRITE,
			// StandardOpenOption.TRUNCATE_EXISTING);
			// ReadableByteChannel lll = Channels.newChannel(res);
			Files.copy(res, bigFile);
			PosixFileAttributeView filesAttView = Files.getFileAttributeView(bigFile, PosixFileAttributeView.class);
			System.out.println(filesAttView);
			Files.setPosixFilePermissions(bigFile, posixFilePermissions);
			// glusterBigFileChannel.transferFrom(lll, 0, lll.);
			System.out.println("Write big file copy end. Size copy:" + Files.size(bigFile));
			// glusterBigFileChannel.close();
		} else {
			System.out.println("Resource not found ");
		}
		// Gluster to temporary
		logger.fine("Big file transferTo start.");
		FileChannel glusterBigFile = FileChannel.open(bigFile, StandardOpenOption.READ);
		logger.fine("Big file transferTo start with size:" + glusterBigFile.size());
		Path temp = Files.createTempFile("pre-", ".txt");
		bigFile.getFileSystem().provider().copy(bigFile, temp, StandardCopyOption.COPY_ATTRIBUTES,
				StandardCopyOption.REPLACE_EXISTING);
		WritableByteChannel out = Channels.newChannel(
				Files.newOutputStream(temp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
		glusterBigFile.transferTo(0, glusterBigFile.size(), out);
		out.close();
		System.out.println("Big file transferTo end to " + temp.toString() + " size: " + Files.size(temp));
		System.out.println("Big file transferTo start.");
		System.out.println("Big file transferTo start with size:" + glusterBigFile.size());
		glusterBigFile.close();
		Path bigFile2Path = bigFile.getFileSystem().getPath("/targetbig2.txt");

		Files.deleteIfExists(bigFile2Path);

		SeekableByteChannel outG = Files.newByteChannel(bigFile2Path, StandardOpenOption.CREATE,
				StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
		glusterBigFile = FileChannel.open(bigFile, StandardOpenOption.READ);
		glusterBigFile.transferTo(0, glusterBigFile.size(), outG);
		outG.close();
		System.out.println("Big file transferTo end to " + temp.toString() + " size: " + Files.size(temp));
		String hello = "Hello, ";
		Path glusterPath = Paths.get(new URI(testFile));
		Files.write(glusterPath, hello.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
		String world = "world!";
		Files.write(glusterPath, world.getBytes(), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
		long bazSize = Files.size(glusterPath);
		System.out.println("SIZE: " + bazSize);
		byte[] readBytes = Files.readAllBytes(glusterPath);
		System.out.println(hello + world + " == " + new String(readBytes));
		System.out.println("Last modified: " + Files.getLastModifiedTime(glusterPath) + " (should be now)");
		fileSystem.provider().checkAccess(glusterPath, AccessMode.READ, AccessMode.WRITE);
		System.out.println("Can read & write file");
		try {
			fileSystem.provider().checkAccess(glusterPath, AccessMode.EXECUTE);
			System.out.println("Uh oh, file is executable, that's bad.");
		} catch (AccessDeniedException e) {
			System.out.println("Can't execute file, that's good.");
		}

		Path symlinkPath = Paths.get(new URI(serverUri + "symlink"));
		Path symlinkTarget = Paths.get(new URI(serverUri + "symlinktarget"));
		if (Files.exists(symlinkPath)) {
			Files.delete(symlinkPath);
		}
		Files.createSymbolicLink(symlinkPath, symlinkTarget);
		System.out.println("SYMLINK: " + symlinkPath.toString() + " => " + Files.readSymbolicLink(symlinkPath));
		Path copyPath = glusterPath.resolveSibling("copy");
		// Files.createFile(copyPath,
		// PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-rw-rw-")));
		if (Files.exists(copyPath)) {
			Files.delete(copyPath);
		}
		Files.copy(glusterPath, copyPath, StandardCopyOption.REPLACE_EXISTING);
		long copySize = Files.size(copyPath);
		System.out.println("Source and copy are " + (bazSize == copySize ? "" : "NOT") + " equal.");
		Path mountPath = Paths.get(new URI(serverUri));
		try {
			Files.newDirectoryStream(mountPath.resolve("bazzzzz"));
		} catch (NotDirectoryException e) {
			System.out.println("Can't list directory of a file, good.");
		}
		DirectoryStream.Filter<? super Path> filter = new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				return entry.endsWith("1");
			}
		};
		DirectoryStream<Path> stream = Files.newDirectoryStream(mountPath, filter);
		System.out.println("Mount contents:");

		for (Path p : stream) {
			System.out.println(p.toString());
		}

		filter = new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				return entry.endsWith("a");
			}
		};
		stream = Files.newDirectoryStream(mountPath, filter);
		System.out.println("Mount contents:");

		for (Path p : stream) {
			System.out.println(p.toString());
		}

		stream = Files.newDirectoryStream(mountPath);
		System.out.println("Mount contents:");

		PathMatcher matcher = fileSystem.getPathMatcher("glob:**/*z");
		for (Path p : stream) {
			System.out.println(p.toString());
			if (matcher.matches(p)) {
				System.out.println(" **** MATCH ****");
			}
		}

		stream = Files.newDirectoryStream(mountPath, "*z");
		System.out.println("Mount contents:");

		for (Path p : stream) {
			System.out.println(p.toString());
		}

		WatchService watchService = fileSystem.newWatchService();
		Path one = Paths.get(new URI(serverUri + "one"));

		System.out.println("STARTSWITH empty: " + one.startsWith("/"));
		Set<PosixFilePermission> posixFilePermissions = PosixFilePermissions.fromString("rwxrwxrwx");
		FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(posixFilePermissions);
		if (Files.exists(one) && Files.isDirectory(one)) {
			FilesHelpers.deleteDirectoryRecursively(one);
		}
		Files.createDirectory(one, attrs);
		one.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
				StandardWatchEventKinds.ENTRY_DELETE);
		for (int i = 0; i < 10; i++) {
			WatchKey take = watchService.poll(1, TimeUnit.SECONDS);
			if (null == take) {
				continue;
			}
			List<WatchEvent<?>> events = take.pollEvents();
			for (WatchEvent e : events) {
				Path path = (Path) e.context();
				Path absolutePath = one.resolve(path).toAbsolutePath();
				boolean exists = Files.exists(absolutePath);
				System.out.println("EXISTS? " + exists);
				if (exists) {
					System.out.println("SIZE: " + Files.size(absolutePath));
				}
				System.out.println(absolutePath);
				System.out.println(e.toString());
			}
			take.reset();
		}
		fileSystem.close();
	}

}
