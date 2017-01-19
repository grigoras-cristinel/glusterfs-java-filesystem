package com.peircean.glusterfs.example;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

public class FilesHelpers {
	private class DeleteVisitor extends SimpleFileVisitor<Path> {

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
			Files.delete(file);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path directory, IOException ioe) throws IOException {
			Files.delete(directory);
			return FileVisitResult.CONTINUE;
		}

	}

	public static void deleteDirectoryRecursively(Path directoryPath) throws IOException {
		DeleteVisitor delelteVisitor = new FilesHelpers().new DeleteVisitor();
		Files.walkFileTree(directoryPath, delelteVisitor);
	}

	public static void copyFile(Path source, Path destination, CopyOption... options) throws IOException {
		if (source == null) {
			throw new IllegalArgumentException("Source cannot be null.");
		}
		if (destination == null) {
			throw new IllegalArgumentException("Destination cannot be null.");
		}
		if (!Files.exists(source)) {
			throw new FileNotFoundException(source.toString());
		}
		if (Files.exists(destination) && !arrayContains(options, StandardCopyOption.REPLACE_EXISTING)) {
			throw new FileAlreadyExistsException(destination.toString());
		}

	}

	private static boolean arrayContains(CopyOption[] options, StandardCopyOption replaceExisting) {
		if (options == null || options.length == 0) {
			return false;
		}
		for (int i = 0; i < options.length; i++) {
			if (options.equals(replaceExisting)) {
				return true;
			}
		}
		return false;
	}

}
