package ru.armagidon.dokitux.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.armagidon.dokitux.utils.StringUtils;

import java.util.List;

public abstract class DokituxCommand extends Command
{

    protected DokituxCommand(String name) {
        super(name);
        Bukkit.getServer().getCommandMap().register("dokitux",this);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if(args.length == 0){
            sender.sendMessage(StringUtils.create(getUsage()).color().replace("<command>",commandLabel).apply());
            return false;
        }
        if(!handle(sender,args)){
            sender.sendMessage(StringUtils.create(getUsage()).color().replace("<command>",commandLabel).apply());
            return false;
        }
        return true;
    }

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return tabComplete(args);
    }

    protected abstract boolean handle(CommandSender sender, String[] args);

    protected abstract List<String> tabComplete(String[] args);
}
