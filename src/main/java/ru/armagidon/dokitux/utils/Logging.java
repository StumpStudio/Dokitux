package ru.armagidon.dokitux.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class Logging
{
    public void severe(CommandSender sender, String message){
        sender.sendMessage(ChatColor.RED+"[SEVERE] "+ChatColor.WHITE+message);
    }

    public void warning(CommandSender sender, String message){
        sender.sendMessage(ChatColor.YELLOW+"[WARNING] "+ChatColor.WHITE+message);
    }

    public void info(CommandSender sender, String message){
        sender.sendMessage(ChatColor.BLUE+"[INFO] "+ChatColor.WHITE+message);
    }

    public void success(CommandSender sender, String message){
        sender.sendMessage(ChatColor.GREEN+"[SUCCESS] "+ChatColor.WHITE+message);
    }
}
