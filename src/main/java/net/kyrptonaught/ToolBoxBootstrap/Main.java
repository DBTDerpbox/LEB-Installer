package net.kyrptonaught.ToolBoxBootstrap;

import com.google.gson.Gson;
import net.kyrptonaught.ToolBoxBootstrap.IO.FileHelper;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static Gson gson = new Gson().newBuilder()
            .setLenient()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    public static void main(String[] args) {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        checkForUpdate(input, args);
    }

    public static void checkForUpdate(BufferedReader input, String[] args) {
        System.out.println("Checking for Toolbox Updates...");
        String installedVersion = UpdateBootstrapper.getInstalledVersion();
        if (installedVersion.equals("0.0")) {
            System.out.println("Toolbox is missing files require to run. The required files will be downloaded automatically.");
            System.out.println();
            System.out.println("1. View Latest Release");
            System.out.println("2. Download");
            System.out.println("0. Exit");
            System.out.println();
            System.out.print("Select Option: ");

            if (containsArgs("--autoUpdateToolbox", args)) {
                System.out.println("Auto Accepting update...");
                UpdateBootstrapper.installUpdate();
                UpdateBootstrapper.runToolbox(args);
                return;
            }

            int selection = readInt(input);
            if (selection == 1) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    try {
                        Desktop.getDesktop().browse(new URI(UpdateBootstrapper.URL));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                clearConsole();
                checkForUpdate(input, args);
            } else if (selection == 2) {
                System.out.println("Installing latest version");
                UpdateBootstrapper.installUpdate();
                UpdateBootstrapper.runToolbox(args);
                return;
            } else if (selection == 0) {
                System.out.println("Exiting...");
                System.exit(0);
            }
        }

        String update = UpdateBootstrapper.isUpdateAvailable();
        if (update != null) {
            Path versionFile = Paths.get(".toolbox/VERSION");
            if (FileHelper.exists(versionFile)) {
                System.out.println("Current version: Toolbox 2.0 v" + installedVersion);
                System.out.println();
                System.out.println("An update for Toolbox is available: v" + update);
                System.out.println();
                System.out.println("1. View Release");
                System.out.println("2. Download Update");
                System.out.println("0. Ignore");
                System.out.println();
                System.out.print("Select Option: ");

                if (containsArgs("--autoUpdateToolbox", args)) {
                    System.out.println("Auto Accepting update...");
                    UpdateBootstrapper.installUpdate();
                    UpdateBootstrapper.runToolbox(args);
                    return;
                }

                int selection = readInt(input);
                if (selection == 1) {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        try {
                            Desktop.getDesktop().browse(new URI(UpdateBootstrapper.URL));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    clearConsole();
                    checkForUpdate(input, args);

                } else if (selection == 2) {
                    System.out.println("Installing update");
                    UpdateBootstrapper.installUpdate();
                    UpdateBootstrapper.runToolbox(args);
                    return;
                }
            }
        }
        System.out.println("Already up to date");
        UpdateBootstrapper.runToolbox(args);
    }

    public static void pressEnterToCont(BufferedReader input) {
        System.out.print("Press ENTER to continue...");
        readLine(input);
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

    public static boolean containsArgs(String arg, String[] args) {
        for (String str : args) {
            if (str.equalsIgnoreCase(arg)) return true;
        }
        return false;
    }
}