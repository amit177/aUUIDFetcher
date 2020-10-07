package systems.amit.aUUIDFetcher.bungee;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import systems.amit.aUUIDFetcher.StringCallback;
import systems.amit.aUUIDFetcher.UUIDCallback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class aUUIDFetcher implements Listener {

    private final String PREFIX = "[aUUIDFetcher v3.1] ";

    private HashMap<String, UUID> uuidCache;
    private HashMap<UUID, String> nameCache;

    private Plugin plugin;

    public aUUIDFetcher(Plugin plugin) {
        this.nameCache = new HashMap<>();
        this.uuidCache = new HashMap<>();

        this.plugin = plugin;
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
        plugin.getLogger().info(PREFIX + "Hooked into " + plugin.getDescription().getName());
    }

    public void fetchUUID(String playerName, UUIDCallback callback) {
        if (uuidCache.containsKey(playerName)) {
            callback.onRetrieve(uuidCache.get(playerName));
        } else if (plugin.getProxy().getPlayer(playerName) != null) {
            ProxiedPlayer p = plugin.getProxy().getPlayer(playerName);
            uuidCache.put(playerName, p.getUniqueId());
            callback.onRetrieve(uuidCache.get(playerName));
        } else {
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                final UUID result = _fetchUUID(playerName);
                plugin.getProxy().getScheduler().schedule(plugin, () -> callback.onRetrieve(result), 1, TimeUnit.MICROSECONDS);
            });
        }
    }

    public UUID fetchUUIDBlocking(String playerName) {
        if (uuidCache.containsKey(playerName)) {
            return uuidCache.get(playerName);
        } else if (plugin.getProxy().getPlayer(playerName) != null) {
            uuidCache.put(playerName, plugin.getProxy().getPlayer(playerName).getUniqueId());
            return uuidCache.get(playerName);
        } else {
            return _fetchUUID(playerName);
        }
    }

    private UUID _fetchUUID(String playerName) {
        UUID result = null;
        HttpURLConnection con = null;
        InputStream is = null;
        BufferedReader br = null;
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            con = (HttpURLConnection) url.openConnection();
            con.connect();

            if (con.getResponseCode() == 200) {
                is = con.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));

                JsonElement parse = new JsonParser().parse(br);
                String resultId = parse.getAsJsonObject().get("id").getAsString();

                String formatUUID = resultId.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
                result = UUID.fromString(formatUUID);
                uuidCache.put(playerName, result);
            } else {
                uuidCache.put(playerName, null);
                plugin.getLogger().severe(PREFIX + "Error while trying to fetch uuid: API response code - " + con.getResponseCode());
            }
        } catch (IOException e) {
            plugin.getLogger().severe(PREFIX + "Error while trying to fetch uuid: could not connect to API");
            e.printStackTrace();
        } finally {
            if (con != null) con.disconnect();
            try { if (is != null) is.close(); } catch (Exception ignored) { }
            try { if (br != null) br.close(); } catch (Exception ignored) { }
        }
        return result;
    }

    public void fetchName(UUID uuid, StringCallback callback) {
        if (nameCache.containsKey(uuid)) {
            callback.onRetrieve(nameCache.get(uuid));
        } else if (plugin.getProxy().getPlayer(uuid) != null) {
            nameCache.put(uuid, plugin.getProxy().getPlayer(uuid).getName());
            callback.onRetrieve(nameCache.get(uuid));
        } else {
            plugin.getProxy().getScheduler().runAsync(plugin, () -> {
                final String result = _fetchName(uuid);
                plugin.getProxy().getScheduler().schedule(plugin, () -> callback.onRetrieve(result), 1, TimeUnit.MICROSECONDS);
            });
        }
    }

    public String fetchNameBlocking(UUID uuid) {
        if (nameCache.containsKey(uuid)) {
            return nameCache.get(uuid);
        } else if (plugin.getProxy().getPlayer(uuid) != null) {
            nameCache.put(uuid, plugin.getProxy().getPlayer(uuid).getName());
            return nameCache.get(uuid);
        } else {
            return _fetchName(uuid);
        }
    }

    private String _fetchName(UUID uuid) {
        String result = null;
        HttpURLConnection con = null;
        InputStream is = null;
        BufferedReader br = null;
        try {
            URL url = new URL("https://api.mojang.com/user/profiles/" + uuid.toString().replace("-", "") + "/names");
            con = (HttpURLConnection) url.openConnection();
            con.connect();

            if (con.getResponseCode() == 200) {
                is = con.getInputStream();
                br = new BufferedReader(new InputStreamReader(is));

                JsonArray parsed = new JsonParser().parse(br).getAsJsonArray();
                JsonObject object = parsed.get(parsed.size() - 1).getAsJsonObject();

                result = object.get("name").getAsString();
                nameCache.put(uuid, result);
            } else {
                nameCache.put(uuid, null);
                plugin.getLogger().severe(PREFIX + "Error while trying to fetch name: API response code - " + con.getResponseCode());
            }
        } catch (IOException e) {
            plugin.getLogger().severe(PREFIX + "Error while trying to fetch name: could not connect to API");
            e.printStackTrace();
        } finally {
            if (con != null) con.disconnect();
            try { if (is != null) is.close(); } catch (Exception ignored) { }
            try { if (br != null) br.close(); } catch (Exception ignored) { }
        }
        return result;
    }

    public HashMap<String, UUID> getUUIDCache() {
        return uuidCache;
    }

    public HashMap<UUID, String> getNameCache() {
        return nameCache;
    }

    @EventHandler
    public void aUUIDFetcher_removePlayer(PlayerDisconnectEvent e) {
        nameCache.remove(e.getPlayer().getUniqueId());
    }
}
