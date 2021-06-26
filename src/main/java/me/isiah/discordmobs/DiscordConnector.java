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

public class DiscordConnector {

    private Random rand = new Random();

    private String accessToken;
    private HttpClient client;

    private GuildDTO guild;
    private List<String> usernames = new ArrayList<String>();
    private Main plugin;

    public DiscordConnector(Main plugin) {
        client = HttpClient.newHttpClient();
        this.plugin = plugin;
        String token = this.plugin.config.getString("access_token");
        if(!token.equals("NO_TOKEN")) {
            this.accessToken = this.plugin.config.getString("access_token");
            this.setGuildId();
            this.sync();
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
                sendToPlayerOrConsole(sender, "[DiscordMobs] This bot is not in any Discord servers, clearing access token");
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
        //TODO: Assert that the SERVER MEMBERS INTENT perm is enabled, and that HTTP errors are caught 
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
                sendToPlayerOrConsole(sender, "[DiscordMobs] Got HTTP 403 Unauthorized, did you remember to enable the SERVER MEMBERS INTENT permission on Discord?");
                return;
            }
            else if(response.statusCode() != 200) {
                sendToPlayerOrConsole(sender, "[DiscordMobs] Got HTTP " + response.statusCode() + ". Is Discord down?");
            }
            GuildMembersDTO[] members = new Gson().fromJson(response.body(), GuildMembersDTO[].class);
            for(GuildMembersDTO member: members) {
                if(this.plugin.config.getBoolean("ignore_bots") && member.user.bot == true) {}
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

    public class GuildDTO {
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
