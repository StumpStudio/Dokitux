package ru.armagidon.dokitux.utils;

import org.bukkit.ChatColor;


public class StringToolsPipeline
{
    private String source;

    private StringToolsPipeline(String source){
        this.source = source;
    }

    public static StringToolsPipeline create(String source){
        return new StringToolsPipeline(source);
    }

    public StringToolsPipeline replace(String replacing, String replacement){
        source = source.replace(replacing, replacement);
        return this;
    }

    public StringToolsPipeline color(){
        source = ChatColor.translateAlternateColorCodes('&',source);
        return this;
    }

    public static void color(String input) {
        ChatColor.translateAlternateColorCodes('&', input);
    }

    public String apply(){
        return source;
    }
}
