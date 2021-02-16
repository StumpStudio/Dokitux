package ru.armagidon.dokitux.utils;

import org.bukkit.ChatColor;

public class StringUtils
{
    private String source;

    private StringUtils(String source){
        this.source = source;
    }

    public static StringUtils create(String source){
        return new StringUtils(source);
    }

    public StringUtils replace(String replacing, String replacement){
        source = source.replace(replacing, replacement);
        return this;
    }

    public StringUtils color(){
        source = ChatColor.translateAlternateColorCodes('&',source);
        return this;
    }

    public String apply(){
        return source;
    }
}
