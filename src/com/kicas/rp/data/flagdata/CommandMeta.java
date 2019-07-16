package com.kicas.rp.data.flagdata;

import com.kicas.rp.RegionProtection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Represents the metadata for a command. This meta is used for the enter-command and exit-command flags, and specifies
 * the sender of the command as well as the command to execute.
 */
public class CommandMeta {
    private boolean isConsole;
    private String command;

    public static final CommandMeta EMPTY_META = new CommandMeta();

    public CommandMeta(boolean isConsole, String command) {
        this.isConsole = isConsole;
        this.command = command;
    }

    public CommandMeta() {
        this(false, "");
    }

    /**
     * Returns whether or not this command is meant to be run by the console.
     * @return true if this command is meant to be run by the console, false otherwise.
     */
    public boolean isConsole() {
        return isConsole;
    }

    public String getCommand() {
        return command;
    }

    /**
     * Executes the command stored in this metadata synchronously. If this command is not run by the console, then the
     * given player is used as the sender of the command. Any occurrences of %0 in the in the command string stored in
     * this meta will be replaced with the given player's username.
     * @param player the player associated with this execution of the command.
     */
    public void execute(Player player) {
        String cmd = command.replaceAll("%0", player.getName());
        Bukkit.getScheduler().runTask(RegionProtection.getInstance(),
                () -> Bukkit.dispatchCommand(isConsole ? Bukkit.getConsoleSender() : player, cmd));
    }
}
