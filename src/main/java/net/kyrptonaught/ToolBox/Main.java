package net.kyrptonaught.ToolBox;

public class Main {
    //Γûä
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--autoHash")) {
            AutoHash.autoHash();
            return;
        }

        Menu.startStateMachine(args);
    }
}