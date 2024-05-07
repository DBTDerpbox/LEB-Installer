package net.kyrptonaught.ToolBoxBootstrap.IO;

import net.kyrptonaught.ToolBoxBootstrap.Main;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class FileHelper {

    public static boolean download(String fileURL, Path savePath) {
        try (InputStream in = openFileOrURL(fileURL)) {
            Files.createDirectories(savePath.getParent());
            Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String download(String fileURL) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openFileOrURL(fileURL)))) {
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = reader.readLine()) != null)
                response.append(inputLine);

            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static <T> T download(String fileURL, Class<T> clazz) {
        return Main.gson.fromJson(download(fileURL), clazz);
    }

    public static List<String> unzipFile(Path zipFile, Path unzipPath, boolean skipTB) {
        List<String> installedFiles = new ArrayList<>();
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            int size = zip.size();

            boolean firstChecked = false;
            String initialDir = null;

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = FileNameCleaner.fixPathSeparator(entry.getName());

                if (!firstChecked) {
                    firstChecked = true;
                    if (entry.isDirectory())
                        initialDir = entryName;
                }

                Path output = unzipPath.resolve(initialDir == null ? entryName : entryName.replace(initialDir, ""));

                //skip toolbox folder in repo
                if (skipTB && (entry.getName().contains(".toolbox") || entry.getName().contains(".github")))
                    continue;

                if (entry.isDirectory())
                    Files.createDirectories(output);
                else {
                    Files.createDirectories(output.getParent());
                    Files.copy(zip.getInputStream(entry), output, StandardCopyOption.REPLACE_EXISTING);
                }
                String installedFile = FileNameCleaner.pathToString(output);
                if (!installedFile.isEmpty() && !installedFile.equals("/"))
                    installedFiles.add(installedFile);
                //System.out.println(count + "/" + size);
            }
            installedFiles.sort(Comparator.reverseOrder());
            return installedFiles;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return installedFiles;
    }

    public static String readFileFromZip(Path zipFile, String fileName) {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            ZipEntry entry = zip.getEntry(fileName);

            return new String(zip.getInputStream(entry).readAllBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void zipDirectory(Path directory, Path zip) {
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zip))) {
            Files.walk(directory)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(directory.relativize(path).toString());
                        try {
                            zs.putNextEntry(zipEntry);
                            Files.copy(path, zs);
                            zs.closeEntry();
                        } catch (IOException e) {
                            System.err.println(e);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static InputStream openFileOrURL(String path) throws IOException {
        path = path.trim();
        if (path.startsWith("file:"))
            return Files.newInputStream(Path.of(path.replace("file:", "")), StandardOpenOption.READ);

        return new URL(path).openStream();
    }

    public static boolean createDir(Path directory) {
        try {
            Files.createDirectories(directory);
            return true;
        } catch (IOException exception) {
            System.out.println("Failed to create directory: " + directory);
            exception.printStackTrace();
        }
        return false;
    }

    public static List<String> moveFile(Path source, Path destination) {
        List<String> installedFiles = new ArrayList<>();
        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            installedFiles.add(FileNameCleaner.pathToString(destination));
        } catch (Exception e) {
            System.out.println("Failed to copy file: " + source + " -> " + destination);
            e.printStackTrace();
        }
        return installedFiles;
    }

    public static List<String> copyFile(Path source, Path destination) {
        List<String> installedFiles = new ArrayList<>();
        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            installedFiles.add(FileNameCleaner.pathToString(destination));
        } catch (Exception e) {
            System.out.println("Failed to copy file: " + source + " -> " + destination);
            e.printStackTrace();
        }
        return installedFiles;
    }

    public static boolean deleteDirectory(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String hashFile(byte[] data) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
            return new BigInteger(1, hash).toString(16);
        } catch (Exception e) {
            System.out.println("Failed to hash file");
            e.printStackTrace();
        }
        return null;
    }

    public static boolean writeFile(Path filePath, String text) {
        try {
            Files.writeString(filePath, text);
            return true;
        } catch (Exception e) {
            System.out.println("Error writing file: " + filePath);
            e.printStackTrace();
        }
        return false;
    }

    public static String readFile(Path filePath) {
        try {
            return Files.readString(filePath);
        } catch (Exception e) {
            System.out.println("Error reading file: " + filePath);
            e.printStackTrace();
        }
        return null;
    }

    public static boolean delete(Path filePath) {
        try {
            Files.delete(filePath);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean exists(Path filePath) {
        return Files.exists(filePath);
    }

    public static boolean renameDirectory(Path original, Path destination) {
        File file = original.toFile();
        file.renameTo(destination.toFile());
        return true;
    }
}
