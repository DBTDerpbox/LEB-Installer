package net.kyrptonaught.ToolBox;

import com.google.gson.JsonObject;
import net.kyrptonaught.ToolBox.IO.ConfigLoader;
import net.kyrptonaught.ToolBox.IO.FileHelper;
import net.kyrptonaught.ToolBox.IO.GithubHelper;
import net.kyrptonaught.ToolBox.configs.BranchConfig;
import net.kyrptonaught.ToolBox.holders.InstalledDependencyInfo;
import net.kyrptonaught.ToolBox.holders.InstalledServerInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class Installer {

    public static List<InstalledServerInfo> detectInstalls() {
        Path installPath = Path.of("installs");

        List<InstalledServerInfo> configs = new ArrayList<>();
        try (Stream<Path> files = Files.walk(installPath, 1)) {
            files.forEach(path -> {
                if (Files.isDirectory(path) && Files.exists(path.resolve(".toolbox").resolve("meta").resolve("toolbox.json"))) {
                    InstalledServerInfo serverInfo = ConfigLoader.parseToolboxInstall(FileHelper.readFile(path.resolve(".toolbox").resolve("meta").resolve("toolbox.json")));
                    serverInfo.setPath(path);
                    configs.add(serverInfo);
                }
            });
        } catch (IOException ignored) {
        }

        return configs;
    }

    public static void installAndCheckForUpdates(InstalledServerInfo serverInfo) {
        FileHelper.createDir(serverInfo.getPath());

        FileHelper.createDir(serverInfo.getMetaPath());
        FileHelper.createDir(serverInfo.getDownloadPath());
        FileHelper.createDir(serverInfo.getInstalledDependencyPath());

        System.out.println("Checking dependencies...");
        installDependencies(serverInfo);

        FileHelper.writeFile(serverInfo.getMetaPath().resolve("toolbox.json"), ConfigLoader.serializeToolboxInstall(serverInfo));
        System.out.println("Dependencies done");
    }

    public static void verifyInstall(InstalledServerInfo serverInfo) {
        for (BranchConfig.Dependency dependency : serverInfo.getDependencies()) {
            System.out.println("Checking files from dependency: " + dependency.getDisplayName());
            InstalledDependencyInfo installedDependency = getInstalledDependency(serverInfo, dependency);
            if (installedDependency.installedFiles != null)
                for (String file : installedDependency.installedFiles)
                    if (!FileHelper.exists(Path.of(file))) {
                        replaceMissingFiles(serverInfo, installedDependency);
                        break;
                    }
        }
    }

    public static void packageInstall(InstalledServerInfo serverInfo) {
        FileHelper.zipDirectory(serverInfo.getPath(), Path.of("packaged/" + serverInfo.getName() + ".toolbox"));
    }

    public static String installPackage(Path path) {
        InstalledServerInfo serverInfo = ConfigLoader.parseToolboxInstall(FileHelper.readFileFromZip(path, ".toolbox\\meta\\toolbox.json"));
        String name = serverInfo.getName() + " (Imported)";

        //todo improve this conflict logic
        while(Menu.getServerFromName(name) !=null){
            name += " (duplicate)";
        }
        serverInfo.setName(name);
        serverInfo.setPath();

        FileHelper.createDir(serverInfo.getPath());
        FileHelper.unzipFile(path, serverInfo.getPath(), false);
        FileHelper.writeFile(serverInfo.getMetaPath().resolve("toolbox.json"), ConfigLoader.serializeToolboxInstall(serverInfo));

        return name;
    }

    private static void installDependencies(InstalledServerInfo serverInfo) {
        for (BranchConfig.Dependency dependency : serverInfo.getDependencies()) {

            if (dependency.location.startsWith("/"))
                dependency.location = dependency.location.substring(1);

            System.out.print("Checking " + dependency.getDisplayName() + "...");

            InstalledDependencyInfo installedDependency = getInstalledDependency(serverInfo, dependency);

            String hash = getNewHash(serverInfo, dependency);
            String existingHash = installedDependency.hash;

            if (hash != null && !hash.equals(existingHash)) {
                System.out.print("downloading...");
                installFile(serverInfo, installedDependency, hash);
                System.out.println("installed");
            } else {
                System.out.println("Already exists");
            }
        }
        detectRemovedDependencies(serverInfo);
    }

    private static String getNewHash(InstalledServerInfo serverInfo, BranchConfig.Dependency dependency) {
        if (dependency.gitRepo) {
            String apiCall = GithubHelper.convertRepoToApiCall(dependency.url);
            JsonObject response = FileHelper.download(apiCall, JsonObject.class);
            return response.getAsJsonObject("commit").getAsJsonPrimitive("sha").getAsString();
        } else {
            Path downloadPath = serverInfo.getDownloadPath(dependency);
            FileHelper.download(dependency.url, downloadPath);
            return FileHelper.hashFile(downloadPath);
        }
    }

    private static void installFile(InstalledServerInfo serverInfo, InstalledDependencyInfo dependency, String hash) {
        Path downloadPath = serverInfo.getDownloadPath(dependency);
        Path destination = serverInfo.getDependencyInstallPath(dependency);
        FileHelper.createDir(destination);

        //checking hash already downloaded other file types
        if (dependency.gitRepo) {
            FileHelper.download(GithubHelper.convertRepoToZipball(dependency.url), downloadPath);
        }

        clearOldFiles(dependency.installedFiles);

        List<String> installedFiles;
        if (dependency.unzip) {
            installedFiles = FileHelper.unzipFile(downloadPath, destination, true);
        } else {
            installedFiles = FileHelper.copyFile(downloadPath, destination.resolve(dependency.name));
        }

        installedFiles.add(destination.toString());
        dependency.installedFiles = installedFiles;
        dependency.hash = hash;
        FileHelper.writeFile(serverInfo.getInstalledDependencyPath(dependency), ConfigLoader.serializeToolboxInstall(dependency));
    }

    private static void replaceMissingFiles(InstalledServerInfo serverInfo, InstalledDependencyInfo dependency) {
        Path downloadPath = serverInfo.getDownloadPath(dependency);
        Path destination = serverInfo.getDependencyInstallPath(dependency);
        FileHelper.createDir(destination);

        if (dependency.unzip) {
            List<String> installedFiles = new ArrayList<>(List.copyOf(dependency.installedFiles));
            if (installedFiles != null) {
                Path unzipPath = serverInfo.getTempPath(dependency);
                FileHelper.unzipFile(downloadPath, unzipPath, true);
                installedFiles.sort(Comparator.naturalOrder());
                for (String file : installedFiles) {
                    if (!FileHelper.exists(Path.of(file))) {
                        System.out.println("Replacing file: " + file);

                        Path path = Path.of(file);
                        if (Files.isDirectory(path)) {
                            FileHelper.createDir(path);
                        } else {
                            FileHelper.copyFile(Path.of(file.replace(serverInfo.getPath().toString(), unzipPath.toString())), path);
                        }
                    }
                }
                FileHelper.deleteDirectory(serverInfo.getTempPath());
            }
        } else {
            System.out.println("Replacing file: " + destination.resolve(dependency.name));
            FileHelper.copyFile(downloadPath, destination.resolve(dependency.name));
        }
    }

    private static void detectRemovedDependencies(InstalledServerInfo serverInfo) {
        try (Stream<Path> dependencyFiles = Files.list(serverInfo.getInstalledDependencyPath())) {
            for (Path dependencyPath : dependencyFiles.toList()) {
                InstalledDependencyInfo installedDependency = ConfigLoader.parseInstalledDependency(FileHelper.readFile(dependencyPath));
                boolean found = false;
                for (BranchConfig.Dependency dependency : serverInfo.getDependencies()) {
                    if (dependency.name.equals(installedDependency.name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    System.out.println("Removing deleted dependency: " + installedDependency.getDisplayName());
                    clearOldFiles(installedDependency.installedFiles);
                    FileHelper.delete(dependencyPath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void clearOldFiles(List<String> previousInstalledFiles) {
        if (previousInstalledFiles != null) {
            for (String string : previousInstalledFiles) {
                FileHelper.delete(Path.of(string));
            }
        }
    }

    private static InstalledDependencyInfo getInstalledDependency(InstalledServerInfo serverInfo, BranchConfig.Dependency dependency) {
        if (FileHelper.exists(serverInfo.getInstalledDependencyPath(dependency))) {
            return ConfigLoader.parseInstalledDependency(FileHelper.readFile(serverInfo.getInstalledDependencyPath(dependency)));
        }
        return new InstalledDependencyInfo(dependency);
    }
}