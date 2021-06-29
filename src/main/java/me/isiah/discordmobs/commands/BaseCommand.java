package me.isiah.discordmobs.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention;
import me.isiah.discordmobs.Main;
import me.isiah.discordmobs.Main.PERM_NODES;


public class BaseCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public BaseCommand(Main plugin) {
        this.plugin = plugin;
    }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> cmplist = new ArrayList<String>();
        if(args.length == 1) {
            if(sender.hasPermission(Main.PERM_STATUS)) {
                cmplist.add("status");
            }
            if(sender.hasPermission(Main.PERM_CONNECT)) {
                cmplist.add("connect");
            }
            if(sender.hasPermission(Main.PERM_SYNC)) {
                cmplist.add("sync");
            }
            if(sender.hasPermission(Main.PERM_RELOAD)) {
                cmplist.add("reload");
            }
        }
        return cmplist;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0 || args[0].equalsIgnoreCase("status")) {
            if(sender.hasPermission(Main.PERM_STATUS)) {
                ComponentBuilder msg = new ComponentBuilder("DiscordMobs").bold(true)
                .append(" v" + plugin.getDescription().getVersion() + "\n").color(ChatColor.GREEN).bold(false);
                if(this.plugin.discord.getGuild() != null) {
                    msg.append("Connected to ").color(ChatColor.WHITE)
                    .append(this.plugin.discord.getGuild().name + "\n")
                    .color(ChatColor.BLUE);
                }
                else {
                    msg.append("No Discord server is connected!\n").color(ChatColor.RED);
                }
                msg.append(this.plugin.discord.getNumMembers() + " Discord members synced").color(ChatColor.WHITE);
                sender.spigot().sendMessage(msg.create());
                sender.spigot().sendMessage(getHelpMsg(sender));
                return true;
            }
        }
        switch(args[0]) {
            case "connect":
                if(sender.hasPermission(Main.PERM_CONNECT)) {
                    if(args.length == 2) {
                        if( this.plugin.discord.setAccessToken(args[1], sender)) {
                            this.plugin.discord.sync(sender);
                        }
                        else {
                            sender.sendMessage(ChatColor.RED + "" + "There was an issue connecting to discord");
                        }
                    }
                    else {
                        sender.spigot().sendMessage(new ComponentBuilder("Click Here").underlined(true).color(ChatColor.AQUA)
                        .event(new ClickEvent(Action.OPEN_URL, "http://discordmobs.isiah.me/"))
                        .append(" to learn how to connect DiscordMobs to your Discord server!", FormatRetention.NONE)
                        .create());
                    }
                    return true;
                }
                else sender.spigot().sendMessage(getNoPermMsg(PERM_NODES.PERM_CONNECT));
                break;
            case "sync":
                if(sender.hasPermission(Main.PERM_SYNC)) {
                    this.plugin.discord.sync(sender);
                    return true;
                }
                else sender.spigot().sendMessage(getNoPermMsg(PERM_NODES.PERM_SYNC));
                break;
            case "reload":
                if(sender.hasPermission(Main.PERM_RELOAD)) {
                    this.plugin.rConfig();
                    sender.sendMessage("[DiscordMobs] Config Reloaded!");
                    return true;
                }
                else sender.spigot().sendMessage(getNoPermMsg(PERM_NODES.PERM_RELOAD));
                break;
            default:
                return false;
        }
        return false;
    }
   private BaseComponent[] getNoPermMsg(PERM_NODES node) {
       return new ComponentBuilder("[DiscordMobs] You don't have permission to use that command!\n")
                                    .color(ChatColor.RED)
                                    .append("You need the ")
                                    .append(node.name())
                                    .color(ChatColor.GOLD)
                                    .append(" permission node")
                                    .color(ChatColor.RED)
                                    .create();

   }
   private BaseComponent[] getHelpMsg(CommandSender sender) {
       ComponentBuilder msg = new ComponentBuilder("Command Help:\n");
       if(sender.hasPermission(Main.PERM_STATUS)) {
            msg.append("/discordmobs (status)").color(ChatColor.GOLD)
            .append(" -- See if DiscordMobs is connected to your Discord server and how many members are synced.\n").color(ChatColor.WHITE);
        }
       if(sender.hasPermission(Main.PERM_CONNECT)) {
            msg.append("/discordmobs connect").color(ChatColor.GOLD)
            .append(" -- Connect DiscordMobs to your Discord server\n").color(ChatColor.WHITE);
       }
       if(sender.hasPermission(Main.PERM_SYNC)) {
            msg.append("/discordmobs sync").color(ChatColor.GOLD)
            .append(" -- Syncs current members on Discord server to DiscordMobs\n").color(ChatColor.WHITE);
       }
       if(sender.hasPermission(Main.PERM_RELOAD)) {
        msg.append("/discordmobs reload").color(ChatColor.GOLD)
        .append(" -- Reloads config.yml from the file system\n").color(ChatColor.WHITE);
   }
       return msg.create();
   }
}
