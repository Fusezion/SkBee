package com.shanebeestudios.skbee.elements.other.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.EventValues;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.server.TabCompleteEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Arrays;

public class TabEvent extends SkriptEvent {

    static {
        Skript.registerEvent("Tab Complete", TabEvent.class, TabCompleteEvent.class,
                "[skbee] tab complete [(of|for) %strings%]")
            .description("Called when a player attempts to tab complete the arguments of a command. ",
                "\nNOTE: Tab complete event is only called for the ARGUMENTS of a command, NOT the command itself.",
                "\nevent-string = the command.")
            .examples("on tab complete of \"/mycommand\":",
                "\tset tab completions for position 1 to \"one\", \"two\" and \"three\"",
                "\tset tab completions for position 2 to 1, 2 and 3",
                "\tset tab completions for position 3 to all players",
                "\tset tab completions for position 4 to (indexes of {blocks::*})", "",
                "on tab complete:",
                "\tif event-string contains \"/ver\":",
                "\t\tclear tab completions")
            .since("1.7.0");
        EventValues.registerEventValue(TabCompleteEvent.class, Player.class, event -> {
            CommandSender sender = event.getSender();
            if (sender instanceof Player) {
                return ((Player) sender).getPlayer();
            }
            return null;
        }, EventValues.TIME_NOW);
        EventValues.registerEventValue(TabCompleteEvent.class, String.class, event -> event.getBuffer().split(" ")[0], EventValues.TIME_NOW);
    }

    private String[] commands;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?> @NotNull [] args, int matchedPattern, @NotNull ParseResult parseResult) {
        commands = args[0] == null ? null : ((Literal<String>) args[0]).getAll();
        return true;
    }

    @Override
    public boolean check(@NotNull Event event) {
        if (commands == null) return true;

        TabCompleteEvent tabEvent = ((TabCompleteEvent) event);
        String command = tabEvent.getBuffer().split(" ")[0];
        if (command.isEmpty()) return false;
        if (command.charAt(0) == '/') {
            command = command.substring(1);
        }
        for (String s : commands) {
            if (s.charAt(0) == '/') {
                s = s.substring(1);
            }
            if (s.equalsIgnoreCase(command)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull String toString(@Nullable Event e, boolean d) {
        return "tab complete" + (commands == null ? "" : " for " + Arrays.toString(commands));
    }

}
