package net.kyrptonaught.ToolBox;

import net.kyrptonaught.ToolBox.IO.ConfigLoader;
import net.kyrptonaught.ToolBox.IO.FileHelper;
import net.kyrptonaught.ToolBox.configs.BranchesConfig;

public class CMDArgsParser {

    public static String[] args;

    public static void setArgs(String[] args) {
        CMDArgsParser.args = args;
    }

    public static boolean containsArgs(String arg) {
        for (String str : args) {
            if (str.equalsIgnoreCase(arg)) return true;
        }
        return false;
    }

    public static boolean containsOrBeginsArgs(String arg) {
        for (String str : args) {
            if (str.equalsIgnoreCase(arg) || str.startsWith(arg)) return true;
        }
        return false;
    }

    public static boolean containsServer() {
        return containsArgs("--server");
    }

    public static String getTargetServer() {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("--server") && !args[i + 1].startsWith("--")) {
                return args[i + 1];
            }
        }
        return null;
    }

    public static boolean autoUpdateToolbox() {return containsArgs("--autoUpdateToolbox");}

    public static boolean updateServer() {
        return containsArgs("--updateServer");
    }

    public static boolean launchServer() {
        return containsArgs("--launchServer");
    }

    public static boolean skipSplash() {
        return containsArgs("--skipSplash");
    }

    public static boolean autoExit() {
        return containsArgs("--autoExit");
    }

    public static boolean autoRestart() {
        return containsArgs("--autoRestart");
    }

    public static boolean updateAll() {
        return containsArgs("--updateAll");
    }

    public static boolean installServer() {
        return containsArgs("--installServer");
    }

    public static int getNewServerRam() {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("--newServerRam") && !args[i + 1].startsWith("--")) {
                return Integer.parseInt(args[i + 1]);
            }
        }
        return 3;
    }

    public static BranchesConfig.BranchInfo getNewServerBranch() {
        BranchesConfig availableBranches = ConfigLoader.parseBranches(FileHelper.download("https://raw.githubusercontent.com/Legacy-Edition-Minigames/ToolBox/java/testConfigs/TestBranches.json"));

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("--newServerBranch") && !args[i + 1].startsWith("--")) {
                String branchURL = args[i + 1];

                for (BranchesConfig.BranchInfo branch : availableBranches.branches) {
                    if (branchURL.equals(branch.url)) {
                        return branch;
                    }
                }
            }
        }
        System.out.println("Using default branch");

        BranchesConfig.BranchInfo defaultBranch = availableBranches.branches.get(0);

        return defaultBranch;
    }

    public static boolean doesNewServerAgreeToEULA() {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("--newServerEULA") && !args[i + 1].startsWith("--")) {
                return Boolean.parseBoolean(args[i + 1]);
            }
        }
        return false;
    }

    public static boolean unattendedInstall() {
        return containsArgs("--unattendedInstall");
    }
}
