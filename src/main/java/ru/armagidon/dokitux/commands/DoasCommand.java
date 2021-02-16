package ru.armagidon.dokitux.commands;

import com.google.common.base.Joiner;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DoasCommand extends DokituxCommand
{

    public DoasCommand() {
        super("doas");
    }

    @Override
    protected boolean handle(CommandSender sender, String[] args) {
        if(args.length < 2) return false;
        String player = args[0];
        Player target = Bukkit.getPlayerExact(player);
        if(target == null) return true;
        String commandLine = Joiner.on(' ').join(Arrays.copyOfRange(args, 1, args.length));
        Bukkit.dispatchCommand(target, commandLine);
        sender.sendMessage("Command dispatched: " + commandLine);
        return true;
    }

    @Override
    public List<String> tabComplete(@NotNull String[] args) {
        if (args.length != 1) return Collections.emptyList();
        return Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).sorted(String.CASE_INSENSITIVE_ORDER).filter(p -> p.startsWith(args[0])).collect(Collectors.toList());
    }
}
