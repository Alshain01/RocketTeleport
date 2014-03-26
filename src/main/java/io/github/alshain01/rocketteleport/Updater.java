package io.github.alshain01.rocketteleport;

/*
* Updater for Bukkit.
*
* This class provides the means to safely and easily update a plugin, or check to see if it is updated using dev.bukkit.org
*/
import java.io.*;
import java.lang.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

final class Updater {
    private static final String PLUGIN_NAME = "RocketTeleport";
    private static final String[] NO_UPDATE_TAG = { "-ALPHA", "-BETA", "-SNAPSHOT" }; // If the version number contains one of these, don't update.

    private final Logger log;
    private final String version;

    private URL url; // Connecting to RSS
    private String apiKey = null; // BukkitDev ServerMods API key
    private UpdateResult result = UpdateResult.NO_UPDATE; // Used for determining the outcome of the update process

    /**
     * Gives the dev the result of the update process. Can be obtained by called getResult().
     */
    public enum UpdateResult {
        /**
         * The updater did not find an update, and nothing was downloaded.
         */
        NO_UPDATE,
        /**
         * For some reason, the updater was unable to contact dev.bukkit.org to download the file.
         */
        FAIL_DBO,
        /**
         * When running the version check, the file on DBO did not contain the a version in the format 'vVersion' such as 'v1.0'.
         */
        FAIL_NOVERSION,
        /**
         * The server administrator has improperly configured their API key in the configuration
         */
        FAIL_BADID,
        FAIL_APIKEY,
        /**
         * The updater found an update, but because of the UpdateType being set to NO_DOWNLOAD, it wasn't downloaded.
         */
        UPDATE_AVAILABLE
    }

    public class UpdateListener implements Listener {
        private final String UPDATE_MESSAGE = "The version of " + PLUGIN_NAME + " that this server is running is out of date. "
                + "Please consider updating to the latest version at dev.bukkit.org/bukkit-plugins/" + PLUGIN_NAME.toLowerCase() + "/.";

        private void notifyUpdate(Player p) {
            if (p.hasPermission("rocketteleport.admin.notifyupdate")) {
                p.sendMessage(ChatColor.DARK_PURPLE + UPDATE_MESSAGE);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        private void onPlayerJoin(PlayerJoinEvent e) {
            if(Updater.this.getResult() == UpdateResult.UPDATE_AVAILABLE) {
                notifyUpdate(e.getPlayer());
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private void onNewUpdateFound(NewUpdateFoundEvent e) {
            for(Player p : Bukkit.getOnlinePlayers()) {
                notifyUpdate(p);
            }

            log.info(UPDATE_MESSAGE);
        }
    }

    public Updater(Plugin plugin) {
        this.version = plugin.getDescription().getVersion();
        this.log = plugin.getLogger();
        long interval = plugin.getConfig().getLong("Update.Interval");
        String key = plugin.getConfig().getString("Update.ServerModsAPIKey");

        if (key == null || key.equalsIgnoreCase("null") || key.equals("")) { key = null; }
        this.apiKey = key;

        try {
            this.url = new URL("https://api.curseforge.com/servermods/files?projectIds=70281");
        } catch (final MalformedURLException e) {
            log.severe("An error occured generating the update URL.");
        }

        plugin.getServer().getPluginManager().registerEvents(new UpdateListener(), plugin);
        if(interval < 1) {
            new UpdateRunnable(plugin.getServer().getPluginManager()).runTaskLaterAsynchronously(plugin, 100);
        } else {
            new UpdateRunnable(plugin.getServer().getPluginManager()).runTaskTimerAsynchronously(plugin, 100, interval * 1200);
        }
    }

    /**
     * Get the result of the update process.
     */
    UpdateResult getResult() {
        return this.result;
    }

    /**
     * Check to see if the program should continue by evaluation whether the plugin is already updated, or shouldn't be updated
     */
    private boolean versionCheck(String title) {
        if (title.split(" v").length == 2) {
            final String remoteVersion = title.split(" v")[1].split(" ")[0]; // Get the newest file's version number
            int remVer, curVer = 0;
            try {
                remVer = this.calVer(remoteVersion);
                curVer = this.calVer(version);
            } catch (final NumberFormatException nfe) {
                remVer = -1;
            }
            if (this.hasTag(version) || version.equalsIgnoreCase(remoteVersion) || (curVer >= remVer)) {
                // We already have the latest version, or this build is tagged for no-update
                this.result = UpdateResult.NO_UPDATE;
                return false;
            }
        } else {
            // The file's name did not contain the string 'vVersion'
            this.log.warning("The updater found a malformed file version. Please notify the author of this error.");
            this.result = UpdateResult.FAIL_NOVERSION;
            return false;
        }
        return true;
    }

    /**
     * Used to calculate the version string as an Integer
     */
    private Integer calVer(String s) throws NumberFormatException {
        if (s.contains(".")) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                final Character c = s.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    sb.append(c);
                }
            }
            return Integer.parseInt(sb.toString());
        }
        return Integer.parseInt(s);
    }

    /**
     * Evaluate whether the version number is marked showing that it should not be updated by this program
     */
    private boolean hasTag(String version) {
        for (final String string : Updater.NO_UPDATE_TAG) {
            if (version.contains(string)) {
                return true;
            }
        }
        return false;
    }

    private String read() {
        try {
            final URLConnection conn = this.url.openConnection();
            conn.setConnectTimeout(5000);

            if (this.apiKey != null) {
                conn.addRequestProperty("X-API-Key", this.apiKey);
            }
            conn.addRequestProperty("User-Agent", PLUGIN_NAME + " Updater");
            conn.setDoOutput(true);

            final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            final String response = reader.readLine();

            final JSONArray array = (JSONArray) JSONValue.parse(response);

            if (array.size() == 0) {
                log.warning("The updater could not find any files for the project " + PLUGIN_NAME + ".");
                this.result = UpdateResult.FAIL_BADID;
                return null;
            }

            return (String) ((JSONObject) array.get(array.size() - 1)).get("name");
        } catch (final IOException e) {
            if (e.getMessage().contains("HTTP response code: 403")) {
                this.log.warning("dev.bukkit.org rejected the API key provided in plugins/" + PLUGIN_NAME + "/config.yml");
                this.log.warning("Please double-check your configuration to ensure it is correct.");
                this.result = UpdateResult.FAIL_APIKEY;
            } else {
                this.log.warning("The updater could not contact dev.bukkit.org for updating.");
                this.log.warning("If you have not recently modified your configuration and this is the first time you are seeing this message, the site may be experiencing temporary downtime.");
                this.result = UpdateResult.FAIL_DBO;
            }
            return null;
        }
    }

    private class UpdateRunnable extends BukkitRunnable {
        private final PluginManager pm;

        UpdateRunnable(PluginManager pm) {
            this.pm = pm;
        }

        @Override
        public void run() {
            String versionName = Updater.this.read();
            if (versionName != null && Updater.this.versionCheck(versionName)) {
                Updater.this.result = UpdateResult.UPDATE_AVAILABLE;
                pm.callEvent(new NewUpdateFoundEvent());
                this.cancel();
            }
        }
    }
}