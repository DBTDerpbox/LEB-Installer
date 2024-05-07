package net.kyrptonaught.ToolBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main {
    //Γûä
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--autoHash")) {
            AutoHash.autoHash();
        } else if (args.length > 0 && args[0].equals("--runToolbox")) {
            Menu.startStateMachine(args);
        } else {
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            CMDArgsParser.setArgs(args);
            Menu.checkForUpdate(input);
        }
    }
}