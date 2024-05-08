package net.kyrptonaught.ToolBox;

import net.kyrptonaught.ToolBox.IO.*;
import net.kyrptonaught.ToolBox.configs.BranchConfig;
import net.kyrptonaught.ToolBox.configs.BranchesConfig;
import net.kyrptonaught.ToolBox.holders.InstalledServerInfo;
import net.kyrptonaught.ToolBox.holders.RunningServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class Menu {
    public static State state;
    public static Object stateData;

    public static void startStateMachine(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println();
            System.out.println();
            System.out.println("SHUTTING DOWN");
            System.out.println("Attempting to stop running servers");
            ServerRunner.exit();
        }));
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        CMDArgsParser.setArgs(args);

        setState(CMDArgsParser.skipSplash() ? State.MENU : State.SPLASH);

        Automation.run();

        clearConsole();

        while (true) {
            clearConsole();
            switch (state) {
                case SPLASH -> splashState(input);
                case MENU -> menuState(input);
                case EXISTING_INSTALL -> existingMenu(input);
                case RUNNING_INSTALL -> runningMenu(input);
                case INSTALLER -> installMenu(input);
                case IMPORT -> importMenu(input);
                case EXIT -> System.exit(0);
            }
        }
    }

    public static void splashState(BufferedReader input) {
        System.out.println(" ▄▄▄     ▄▄▄▄▄▄▄ ▄▄   ▄▄    ▄▄▄▄▄▄▄ ▄▄▄▄▄▄▄ ▄▄▄▄▄▄▄ ▄▄▄     ▄▄▄▄▄▄▄ ▄▄▄▄▄▄▄ ▄▄   ▄▄ ");
        System.out.println("█   █   █       █  █▄█  █  █       █       █       █   █   █  ▄    █       █  █▄█  █");
        System.out.println("█   █   █    ▄▄▄█       █  █▄     ▄█   ▄   █   ▄   █   █   █ █▄█   █   ▄   █       █");
        System.out.println("█   █   █   █▄▄▄█       █    █   █ █  █ █  █  █ █  █   █   █       █  █ █  █       █");
        System.out.println("█   █▄▄▄█    ▄▄▄█       █    █   █ █  █▄█  █  █▄█  █   █▄▄▄█  ▄   ██  █▄█  ██     █ ");
        System.out.println("█       █   █▄▄▄█ ██▄██ █    █   █ █       █       █       █ █▄█   █       █   ▄   █");
        System.out.println("█▄▄▄▄▄▄▄█▄▄▄▄▄▄▄█▄█   █▄█    █▄▄▄█ █▄▄▄▄▄▄▄█▄▄▄▄▄▄▄█▄▄▄▄▄▄▄█▄▄▄▄▄▄▄█▄▄▄▄▄▄▄█▄▄█ █▄▄█");
        System.out.println();
        System.out.println("Welcome to LEM-ToolBox!");
        System.out.println();
        System.out.println("LEM-ToolBox is a tool designed to make it easy for users to install, update and customize their own LEM instance.");
        System.out.println();
        System.out.println("If you encounter any problem, try performing a clean reinstall or contact us on our Discord.");
        System.out.println("LEM-ToolBox 2.0 created by Kyrptonaught");
        System.out.println("Legacy Edition Minigames created by DBTDerpbox & Kyrptonaught");
        System.out.println("Consider donating at Patreon! www.legacyminigames.net/patreon");
        System.out.println();
        System.out.println("Have fun!");
        System.out.println();

        pressEnterToCont(input);

        setState(State.MENU);
    }

    public static void menuState(BufferedReader input) {
        System.out.println("Loading Servers...");
        System.out.println();

        List<InstalledServerInfo> installedServers = Installer.detectInstalls();
        List<RunningServer> runningServers = ServerRunner.getRunningServers();
        BranchesConfig branches = ConfigLoader.parseBranches(FileHelper.download("https://raw.githubusercontent.com/Legacy-Edition-Minigames/ToolBox/java/DEFAULT_BRANCHES.json"));

        HashMap<Integer, Runnable> options = new HashMap<>();

        clearConsole();

        System.out.println("Select the server you would like to use");
        int serverOptions = 0;

        System.out.println();
        System.out.println("Installed Servers");
        if (!installedServers.isEmpty()) {
            for (InstalledServerInfo serverInfo : installedServers) {
                if (runningServers.stream().anyMatch(runningServer -> runningServer.serverInfo.getName().equals(serverInfo.getName())))
                    continue;
                serverOptions++;
                System.out.println(serverOptions + ". " + serverInfo.getName() + " (" + serverInfo.getBranchInfo().name + ")");
                options.put(serverOptions, () -> setState(State.EXISTING_INSTALL, serverInfo));
            }
        }

        if (serverOptions == 0) {
            System.out.println("--NONE--");
        }

        System.out.println();
        System.out.println("Running Servers");
        if (!runningServers.isEmpty()) {
            for (RunningServer runningServer : runningServers) {
                serverOptions++;
                System.out.println(serverOptions + ". " + runningServer.serverInfo.getName() + " (" + runningServer.serverInfo.getBranchInfo().name + ")");
                options.put(serverOptions, () -> setState(State.RUNNING_INSTALL, runningServer));
            }
        } else {
            System.out.println("--NONE--");
        }

        System.out.println();
        System.out.println("New Servers");
        if (!branches.branches.isEmpty()) {
            for (BranchesConfig.BranchInfo branch : branches.branches) {
                serverOptions++;
                System.out.println(serverOptions + ". " + branch.name + " : " + branch.desc);
                options.put(serverOptions, () -> setState(State.INSTALLER, branch));
            }
        } else {
            System.out.println("--NONE--");
        }

        System.out.println();
        System.out.println("Import Servers");
        System.out.println(++serverOptions + ". Import a packaged .toolbox server");
        options.put(serverOptions, () -> setState(State.IMPORT));

        System.out.println();
        System.out.println("Other Options");
        System.out.println("0. Exit");

        options.put(0, () -> setState(State.EXIT));

        System.out.println();

        System.out.print("Select Server: ");
        int selection = readInt(input);


        if (options.containsKey(selection)) {
            options.get(selection).run();
        }
        //will loop back into this menu
    }

    public static void existingMenu(BufferedReader input) {
        InstalledServerInfo serverInfo = (InstalledServerInfo) stateData;

        BranchesConfig.BranchInfo info = serverInfo.getBranchInfo();
        System.out.println("Server Selected: ");
        System.out.println();
        System.out.println(serverInfo.getName() + " (" + info.name + ")");
        System.out.println(info.desc);
        System.out.println(info.url);
        System.out.println();
        System.out.println("""
                Choose an action below:
                                
                1. Start Server
                2. Check for Updates
                3. Verify Integrity
                4. Share Install
                5. Accept EULA
                6. Rename Server
                7. Reinstall
                8. Delete
                                
                0. Back
                 """);

        System.out.print("Action: ");
        int selectedAction = readInt(input);
        System.out.println();

        if (selectedAction == 0) {
            setState(State.MENU);
        } else if (selectedAction == 1) {
            clearConsole();
            Executer.startServer(serverInfo);
            setState(State.MENU);
            pressEnterToCont(input);
        } else if (selectedAction == 2) {
            Executer.updateServer(serverInfo);
            System.out.println();
            System.out.println("Server updated.");
            System.out.println();
            pressEnterToCont(input);
        } else if (selectedAction == 3) {
            System.out.println("Verifying install...");
            System.out.println();
            Installer.verifyInstall(serverInfo);
            System.out.println();

            System.out.println("All checks passed, server install is intact");
            System.out.println();
            pressEnterToCont(input);
        } else if (selectedAction == 4) {
            System.out.println("Packaging install...");
            System.out.println();
            FileHelper.createDir(Path.of("packaged"));
            Installer.packageInstall(serverInfo);
            System.out.println("Placed packaged server in: " + Path.of("packaged/" + serverInfo.getName() + ".toolbox"));
            System.out.println();
            pressEnterToCont(input);
        } else if (selectedAction == 5) {
            System.out.println("Checking EULA...");
            System.out.println();
            checkEula(input, serverInfo);
        } else if (selectedAction == 6) {
            String newName = askServerName(input, serverInfo.getName());

            System.out.println();
            System.out.println("Renaming to " + newName);
            FileHelper.renameDirectory(serverInfo.getPath(), Path.of("installs/" + newName));
            serverInfo.setName(newName);
            serverInfo.setPath();
            Installer.saveInstalledServerInfo(serverInfo);
            System.out.println("Complete");
            System.out.println();
        } else if (selectedAction == 7) {
            System.out.println("This server and all data associated with it will be permanently deleted before being reinstalled.");
            System.out.println("This is irreversible.");
            System.out.println();
            System.out.print("Are you sure you want to reinstall this server? (Y/N): ");
            String deleteAgree = readLine(input);
            System.out.println();

            if (!deleteAgree.isEmpty() && deleteAgree.substring(0, 1).equalsIgnoreCase("Y")) {
                FileHelper.deleteDirectory(serverInfo.getPath());
                System.out.println("Server deleted...reinstalling...");
                System.out.println();
                Installer.installAndCheckForUpdates(serverInfo);
                System.out.println();
                checkEula(input, serverInfo);
                System.out.println("Server reinstalled.");
                System.out.println();
                pressEnterToCont(input);
            }
        } else if (selectedAction == 8) {
            System.out.println("This server and all data associated with it will be permanently deleted.");
            System.out.println("This is irreversible.");
            System.out.println();
            System.out.print("Are you sure you want to delete this server? (Y/N): ");
            String deleteAgree = readLine(input);
            System.out.println();

            if (!deleteAgree.isEmpty() && deleteAgree.substring(0, 1).equalsIgnoreCase("Y")) {
                FileHelper.deleteDirectory(serverInfo.getPath());
                System.out.println("Server deleted.");
                System.out.println();
                pressEnterToCont(input);
                setState(State.MENU);
            }
        }
        //will loop back into this menu
    }

    public static void runningMenu(BufferedReader input) {
        RunningServer runningServer = (RunningServer) stateData;

        BranchesConfig.BranchInfo info = runningServer.serverInfo.getBranchInfo();
        System.out.println("Server Selected: ");
        System.out.println();
        System.out.println(runningServer.serverInfo.getName() + " (" + info.name + ")");
        System.out.println(info.desc);
        System.out.println(info.url);
        System.out.println();
        System.out.println("""
                Choose an action below:
                                
                1. Open Console
                2. Stop Server
                                
                0. Back
                 """);

        System.out.print("Action: ");
        int selectedAction = readInt(input);
        System.out.println();

        if (selectedAction == 0) {
            setState(State.MENU);
        } else if (selectedAction == 1) {
            clearConsole();
            System.out.println("Resuming server");
            ServerRunner.resumeServer(runningServer);

            System.out.println();
            if (runningServer.isRunning()) {
                System.out.println("Server backgrounded...");

            } else {
                System.out.println("Server stopped...");
            }
            System.out.println();

            setState(State.MENU);
            pressEnterToCont(input);
        } else if (selectedAction == 2) {
            System.out.println();
            System.out.println("Stopping Server...");

            ServerRunner.stopServer(runningServer);
            System.out.println();
            System.out.println("Server Stopped...");
            System.out.println();
            setState(State.MENU);
            pressEnterToCont(input);
        }
    }

    public static void installMenu(BufferedReader input) {
        BranchesConfig.BranchInfo branchInfo = (BranchesConfig.BranchInfo) stateData;

        System.out.println("Loading branch: " + branchInfo.name + " (" + branchInfo.url + ")");
        System.out.println();

        String url = GithubHelper.convertRepoToToolboxConfig(branchInfo.url);
        BranchConfig branch = ConfigLoader.parseToolboxConfig(FileHelper.download(url));

        if (branch == null) {
            System.out.println();
            System.out.println("This branch is invalid.");
            System.out.println("Returning to menu.");
            pressEnterToCont(input);

            setState(State.MENU);
            return;
        }

        System.out.println("Configuring server");
        System.out.println();

        String enteredServerName = askServerName(input, branch.name);
        System.out.println();

        System.out.println("How much RAM do you want to allocate to the server?");
        System.out.println("It's recommended to use at least 3GB of RAM to ensure LEM will work as intended.");
        System.out.println();
        System.out.print("RAM Allocation (GB): ");
        int allocatedRam = readInt(input);
        System.out.println();

        InstalledServerInfo serverInfo = new InstalledServerInfo(branch, branchInfo);
        serverInfo.setName(enteredServerName);
        serverInfo.setPath();
        if (allocatedRam < 1) allocatedRam = 3;
        serverInfo.setRAMArgs(allocatedRam);

        System.out.println("Creating toolbox instance in " + serverInfo.getPath());
        Installer.installAndCheckForUpdates(serverInfo);
        System.out.println("Finished");
        System.out.println();
        checkEula(input, serverInfo);
        pressEnterToCont(input);

        setState(State.EXISTING_INSTALL, serverInfo);

        //will loop back into this menu
    }

    public static void importMenu(BufferedReader input) {
        System.out.println("Import a packaged .toolbox server");
        System.out.println();

        System.out.println("""
                Choose how to import:
                                
                1. Discover in /install folder
                2. Select from local file system
                3. Download from URL
                                
                0. Back
                 """);

        System.out.print("Option: ");
        int selectedAction = readInt(input);
        System.out.println();

        if (selectedAction == 0) {
            setState(State.MENU);
        } else if (selectedAction == 1) {
            System.out.println("Detecting .toolbox packages in /installs...");
            System.out.println();

            Path installPath = Path.of("installs");
            try (Stream<Path> files = Files.walk(installPath, 1)) {
                files.forEach(path -> {
                    if (!Files.isDirectory(path) && path.toString().endsWith(".toolbox")) {
                        System.out.println("Installing: " + path.getFileName().toString());
                        String importedName = Installer.installPackage(path);
                        FileHelper.delete(path);
                        System.out.println("Installed as: " + importedName);
                        System.out.println();
                    }
                });
            } catch (IOException ignored) {
            }
            System.out.println();
            System.out.println("Finished importing");
            pressEnterToCont(input);
            setState(State.MENU);
        } else if (selectedAction == 2) {
            System.out.println("Please enter the path of the .toolbox file (You can also drag and drop the file here)");
            System.out.print("Path: ");
            String path = readLine(input).trim();
            path = path.replaceAll("^[\"|']|[\"|']$", "");

            System.out.println();
            System.out.println("Installing");

            Path tempDownload = Path.of("tempDownload").resolve("temp.toolbox");
            FileHelper.download("file:" + path, tempDownload);
            String importedName = Installer.installPackage(tempDownload);
            FileHelper.deleteDirectory(Path.of("tempDownload"));

            System.out.println();
            System.out.println("Imported server as: " + importedName);
            pressEnterToCont(input);
            setState(State.MENU);
        } else if (selectedAction == 3) {
            System.out.println("Please enter the URL of the .toolbox file");
            System.out.print("URL: ");
            String path = readLine(input).trim();
            path = path.replaceAll("^[\"|']|[\"|']$", "");

            System.out.println();
            System.out.println("Installing");

            Path tempDownload = Path.of("tempDownload").resolve("temp.toolbox");
            FileHelper.download(path, tempDownload);
            String importedName = Installer.installPackage(tempDownload);
            FileHelper.deleteDirectory(Path.of("tempDownload"));

            System.out.println();
            System.out.println("Imported server as: " + importedName);
            pressEnterToCont(input);
            setState(State.MENU);
        }
    }

    public static String askServerName(BufferedReader input, String defaultName) {
        System.out.println("Please enter a name for this server, or leave blank for default (" + defaultName + "): ");
        System.out.println();
        System.out.print("Server Name: ");
        String enteredServerName = readLine(input);
        if (enteredServerName.isBlank()) enteredServerName = defaultName;
        enteredServerName = FileNameCleaner.cleanFileName(enteredServerName);

        if (getServerFromName(enteredServerName) != null) {
            System.out.println("A server with that name already exists.");
            System.out.println();
            return askServerName(input, defaultName);
        }
        return enteredServerName;
    }

    public static void checkEula(BufferedReader input, InstalledServerInfo serverInfo) {
        Path eulaFile = serverInfo.getPath().resolve("eula.txt");
        boolean agreed = EulaChecker.checkEulaAgreement(eulaFile);

        if (agreed) return;

        System.out.println("""
                Do you accept the Minecraft's EULA?
                                
                For your server to run you must accept Minecraft's EULA.
                The Minecraft's EULA contains information and rules about what you can do and can't do while using the game.
                Agreement of the Minecraft's EULA is strictly needed, otherwise your server would be illegal to operate and thus, won't open.
                You can read the EULA here: https://aka.ms/MinecraftEULA
                WARNING: LEM WON'T WORK IF MINECRAFT'S EULA IS NOT AGREED!
                """);
        System.out.print("Do you want to accept the Minecraft's EULA? (Y/N): ");
        String eulaAgree = readLine(input);

        if (!eulaAgree.isEmpty() && eulaAgree.substring(0, 1).equalsIgnoreCase("Y")) {
            EulaChecker.agreeToEula(eulaFile);
            System.out.println("EULA accepted.");
        }
        System.out.println();
    }

    public static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                System.out.print("\033\143");
        } catch (Exception ignored) {
        }
    }

    public static void pressEnterToCont(BufferedReader input) {
        System.out.print("Press ENTER to continue...");
        readLine(input);
    }

    public static String readLine(BufferedReader input) {
        try {
            return input.readLine().trim();
        } catch (Exception ignored) {
        }
        return "";
    }

    public static int readInt(BufferedReader input) {
        try {
            return Integer.parseInt(input.readLine());
        } catch (NumberFormatException numberFormatException) {
            System.out.print("Please enter a number: ");
            return readInt(input);
        } catch (Exception ignored) {
        }
        return -1;
    }

    public static InstalledServerInfo getServerFromName(String name) {
        List<InstalledServerInfo> serverInfos = Installer.detectInstalls();

        for (InstalledServerInfo serverInfo : serverInfos) {
            if (serverInfo.getName().equals(name)) return serverInfo;
        }

        return null;
    }

    public static void setState(State state) {
        setState(state, null);
    }

    public static void setState(State state, Object stateData) {
        Menu.state = state;
        Menu.stateData = stateData;
    }

    public enum State {
        SPLASH,
        MENU,
        EXISTING_INSTALL,
        RUNNING_INSTALL,
        INSTALLER,
        IMPORT,
        EXIT
    }
}