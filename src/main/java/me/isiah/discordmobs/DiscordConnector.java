package me.isiah.discordmobs;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;

import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention;

public class DiscordConnector {

    private Random rand = new Random();

    private String accessToken;
    private HttpClient client;

    private GuildDTO guild;
    private List<String> usernames = new ArrayList<String>();
    private List<String> roleIds = new ArrayList<String>();
    private Main plugin;

    public DiscordConnector(Main plugin) {
        client = HttpClient.newHttpClient();
        this.plugin = plugin;
        String token = this.plugin.config.getString("access_token");
        if(!token.equals("NO_TOKEN")) {
            this.accessToken = this.plugin.config.getString("access_token");
            this.setGuildId();
            convertRoleNamesToIds(null, plugin.config.getStringList("roles"));
            this.sync();
        }
    }

    public boolean convertRoleNamesToIds(CommandSender sender, List<String> roleNames) {
        URI url = null;
        roleIds.clear();
        if(roleNames.size() == 0) {
            return false;
        }
        try {
            url = new URI(String.format("https://discord.com/api/guilds/%s/roles", this.guild.id));
        }
        catch(URISyntaxException e) {
            this.plugin.getLogger().warning("There was an error while constructing Roles URI: " + e.toString());
        }
        HttpRequest request = HttpRequest.newBuilder(url)
                                        .header("Authorization", "Bot " + this.accessToken)
                                        .setHeader("User-Agent", "DiscordMobs (plugins@isiah.me)")
                                        .GET()
                                        .build();
        try {
            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                sendToPlayerOrConsole(sender, "[DiscordMobs] Got HTTP 401 Unauthorized, I thinks your access code may be wrong.");
                return false;
            }
            else if (response.statusCode() != 200) {
                sendToPlayerOrConsole(sender, "[DiscordMobs] Got HTTP " + response.statusCode() + " while getting guilds, is Discord down?");
            }

            RoleDTO[] roles = new Gson().fromJson(response.body(), RoleDTO[].class);
            Integer numRolesSynced = 0;
            for(int i=0; i < roles.length; i++) {
                RoleDTO role = roles[i];
                if (roleNames.contains(role.name)) {
                    roleIds.add(role.id);
                    numRolesSynced++;
                }
            }
            sendToPlayerOrConsole(sender, "[DiscordMobs] " + numRolesSynced + " roles synced");
            return true;
        }
        catch(Exception e) {
            sendToPlayerOrConsole(sender, "[DiscordMobs] There was an error while fetching roles: " + e.toString());
            return false;
        }
    }

    public boolean setAccessToken(String token, CommandSender sender) {
        this.accessToken = token;
        this.plugin.config.set("access_token", token);
        if(this.setGuildId(sender)) {
            return true;
        }
        this.plugin.sConfig();
        return false;

    }
    public GuildDTO getGuild() {
        return this.guild;
    }
    public Boolean isMembersLoaded() {
        return this.usernames.size() > 0;
    }
    private boolean setGuildId() {
        return setGuildId(null);
    }
    private boolean setGuildId(CommandSender sender) {
        if(this.accessToken.equals("NO_TOKEN")) {
            this.plugin.getLogger().warning("[DiscordMobs] Can't set Guild Id without access token");
            return false;
        }
        URI url = null;
        try {
            url = new URI("https://discord.com/api/users/@me/guilds?limit=1");
        }
        catch(URISyntaxException e) {
            this.plugin.getLogger().warning("There was an error while constructing Guild URI: " + e.toString());
        }
        HttpRequest request = HttpRequest.newBuilder(url)
                                        .header("Authorization", "Bot " + this.accessToken)
                                        .setHeader("User-Agent", "DiscordMobs (plugins@isiah.me)")
                                        .GET()
                                        .build();
        try {
            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                sendToPlayerOrConsole(sender, "[DiscordMobs] Got HTTP 401 Unauthorized, I thinks your access code may be wrong.");
                return false;
            }
            else if (response.statusCode() != 200) {
                sendToPlayerOrConsole(sender, "[DiscordMobs] Got HTTP " + response.statusCode() + " while getting guilds, is Discord down?");
            }

            GuildDTO[] guilds = new Gson().fromJson(response.body(), GuildDTO[].class);
            if(guilds.length > 0) {
                if(guilds.length > 1) {
                    sendToPlayerOrConsole(sender, "[DiscordMobs] This bot is in multiple servers, choosing: " + guilds[0].name);
                }
                this.guild = guilds[0];
                return true;
            }
            else {
                BaseComponent[] msg = new ComponentBuilder("[DiscordMobs] This bot is not in any Discord servers, clearing access token!")
                                            .color(ChatColor.RED)
                                            .append("\nClick here").underlined(true).color(ChatColor.AQUA)
                                            .event(new ClickEvent(Action.OPEN_URL, "http://discordmobs.isiah.me/"))
                                            .append(" to learn how to connect DiscordMobs to your Discord server!", FormatRetention.NONE)
                                            .create();
                sendToPlayerOrConsole(sender, msg, "[DiscordMobs] This bot is not in any Discord servers, clearing access token!\nGo to http://discordmobs.isiah.me/ to learn how to connect DiscordMobs to your Discord server!");
                this.accessToken = null;
                return false;
            }
        }
        catch(Exception e) {
            sendToPlayerOrConsole(sender, "There was an error while fetching Guilds: " + e.toString());
            return false;
        }
    }
    public void sync() {
        this.sync(null);
    }

    public void sync(CommandSender sender) throws NullPointerException {
        this.usernames.clear();
        if(this.guild == null) {
            this.plugin.getLogger().warning("Can not sync members, as no Guild is set");
            throw new NullPointerException();
        }
        sendToPlayerOrConsole(sender, "Syncing members from " + this.guild.name);
        URI url = null;
        try {
            url = new URI(String.format("https://discord.com/api/guilds/%s/members?limit=1000", this.guild.id));
        }
        catch(URISyntaxException e) {
            this.plugin.getLogger().warning("There was an error while constructing Guild URI: \n" + e.toString());
        }
        HttpRequest request = HttpRequest.newBuilder(url)
                                        .header("Authorization", "Bot " + this.accessToken)
                                        .setHeader("User-Agent", "DiscordMobs (plugins@isiah.me)")
                                        .GET()
                                        .build();
        try {
            HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == 403) {
                BaseComponent[] msg = new ComponentBuilder("[DiscordMobs] Got HTTP 403 Unauthorized, did you remember to enable the SERVER MEMBERS INTENT permission on Discord?")
                                      .color(ChatColor.RED)
                                      .append("\nClick here").underlined(true).color(ChatColor.AQUA)
                                      .event(new ClickEvent(Action.OPEN_URL, "http://discordmobs.isiah.me/"))
                                      .append(" to learn how to connect DiscordMobs to your Discord server!", FormatRetention.NONE)
                                      .create();
                sendToPlayerOrConsole(sender, msg, "[DiscordMobs] Got HTTP 403 Unauthorized, did you remember to enable the SERVER MEMBERS INTENT permission on Discord?\nGo to http://discordmobs.isiah.me/ to learn how to connect DiscordMobs to your Discord server!");
                return;
            }
            else if(response.statusCode() != 200) {
                sendToPlayerOrConsole(sender, "[DiscordMobs] Got HTTP " + response.statusCode() + ". Is Discord down?");
            }
            GuildMembersDTO[] members = new Gson().fromJson(response.body(), GuildMembersDTO[].class);
            for(GuildMembersDTO member: members) {
                if(this.plugin.config.getBoolean("ignore_bots") && member.user.bot == true) continue;
                if(roleIds.size() > 0 ) {
                    for(int i=0; i < member.roles.length; i++) {
                        if(roleIds.contains(member.roles[i])) {
                            this.usernames.add(member.user.username);
                            break;
                        }
                    }
                }
                else this.usernames.add(member.user.username);
            }
            sendToPlayerOrConsole(sender, "Synced " + usernames.size() + " members");
        }
        catch(Exception e) {
            sendToPlayerOrConsole(sender, "There was an error while fetching members, consult log for more info.");
        }
    }

    public String getRandomUsername() {
        return this.usernames.get(this.rand.nextInt(this.usernames.size()));
    }
    public int getNumMembers() {
        return this.usernames.size();
    }
    public void sendToPlayerOrConsole(CommandSender sender, String msg) {
        if (sender == null) {
            this.plugin.getLogger().info(msg);
        }
        else {
            sender.sendMessage(msg);
        }
    }
    public void sendToPlayerOrConsole(CommandSender sender, BaseComponent[] msg, String msgString) {
        if (sender == null) {
            this.plugin.getLogger().info(msgString);
        }
        else {
            sender.spigot().sendMessage(msg);
        }
    }

    public class GuildDTO {
        public String id;
        public String name;
    }
    public class RoleDTO {
        public String id;
        public String name;
    }
    private class GuildMembersDTO {
        String[] roles;
        UserInfoDTO user;
        private class UserInfoDTO {
            String username;
            boolean bot; 
        }
    }
}
