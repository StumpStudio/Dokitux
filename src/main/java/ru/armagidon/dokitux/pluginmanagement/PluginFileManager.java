package ru.armagidon.dokitux.pluginmanagement;

import com.google.gson.*;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import ru.armagidon.dokitux.utils.DownloadCallback;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.StreamSupport;

public class PluginFileManager
{

    private @Getter final Map<String, File> PLUGIN_FILE_REGISTRY = new HashMap<>();
    private final File PLUGINS_FOLDER;
    private final File ACCOUNT_FILE;
    @Getter private final Map<String, PluginFile> accountedPlugins = new HashMap<>();
    @Getter private final Plugin plugin;
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(PluginFile.class, PluginFile.SERIALIZER).setPrettyPrinting().create();

    private static final JsonParser PARSER = new JsonParser();

    public PluginFileManager(Plugin plugin) {
        this.plugin = plugin;
        PLUGINS_FOLDER = new File(plugin.getDataFolder().getParent());

        File[] content = PLUGINS_FOLDER.listFiles();
        if(PLUGINS_FOLDER.isDirectory() && content != null) {
            Arrays.stream(content).forEach(file -> {
                try{
                    PluginDescriptionFile pluginYAML = plugin.getPluginLoader().getPluginDescription(file);
                    PLUGIN_FILE_REGISTRY.put(pluginYAML.getName(), file);
                } catch (InvalidDescriptionException ignored){}
            });
        }
        this.ACCOUNT_FILE = new File(plugin.getDataFolder(), "account.json");
    }

    public void putPluginToAccount(PluginDescriptionFile descriptionFile, PluginFile file) {
        accountedPlugins.put(descriptionFile.getName(), file);
        PLUGIN_FILE_REGISTRY.put(file.getName(), file.getFile());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ACCOUNT_FILE))) {
            writer.write(gson.toJson(accountedPlugins.values()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**Deletes, replaces, downloads plugins*/

    public boolean erasePlugin(String pluginName){
        plugin.getLogger().info("Deleting plugin " + pluginName + "...");
        if(!PLUGIN_FILE_REGISTRY.containsKey(pluginName)){
            plugin.getLogger().severe("Plugin not found");
            return false;
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin != null)
            Bukkit.getServer().getPluginManager().disablePlugin(plugin, true);
        return PLUGIN_FILE_REGISTRY.get(pluginName).delete();
    }



    @SneakyThrows
    public List<PluginFile> findPlugin(String name) {
        //Get plugin id
        final List<PluginFile> pluginDataList = new ArrayList<>();
        final String url = "https://api.spiget.org/v2/search/resources/" + name + "?field=name&fields=file%2Cid%2Cauthor%2Cname%2CtestedVersions%2Cversions";
        HttpURLConnection fetchPluginData = makeConnection(url);
        try (BufferedReader pluginDataOut = new BufferedReader(new InputStreamReader(fetchPluginData.getInputStream()))) {
            JsonArray pluginsFound =  PARSER.parse(pluginDataOut).getAsJsonArray();

            for (JsonElement element : pluginsFound) {
                JsonObject data = element.getAsJsonObject();

                HttpURLConnection fetchPluginAuthor = makeConnection("https://api.spiget.org/v2/authors/" + data.get("author").getAsJsonObject().get("id").getAsInt());
                String author;
                try (BufferedReader pluginAuthorOut = new BufferedReader(new InputStreamReader(fetchPluginAuthor.getInputStream()))) {
                    JsonObject obj = PARSER.parse(pluginAuthorOut).getAsJsonObject();
                    author = obj.get("name").getAsString();
                } catch (Exception e) {
                    continue;
                } finally {
                    fetchPluginAuthor.disconnect();
                }

                HttpURLConnection fetchPluginVersion = makeConnection("https://api.spiget.org/v2/resources/" + data.get("id").getAsInt() + "/versions/latest");
                String version;
                try (BufferedReader versionReader = new BufferedReader(new InputStreamReader(fetchPluginVersion.getInputStream()))) {
                    JsonObject versions = PARSER.parse(versionReader).getAsJsonObject();
                    version = versions.get("name").getAsString();
                }

                JsonObject fileData = data.get("file").getAsJsonObject();
                if (!fileData.get("type").getAsString().trim().equalsIgnoreCase(".jar")) continue;

                pluginDataList.add(PluginFile.builder().author(author)
                        .id(data.get("id").getAsInt())
                        .name(data.get("name").getAsString())
                        .version(version)
                        .supportedVersions(StreamSupport.stream(data.get("testedVersions").getAsJsonArray().spliterator(), false)
                                .map(JsonElement::getAsString).toArray(String[]::new))
                        .build());

            }


        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            fetchPluginData.disconnect();
        }


        return pluginDataList;
    }

    public File getPluginsFolder() {
        return PLUGINS_FOLDER;
    }

    public static HttpURLConnection makeConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "DokituxAgent");
        connection.connect();
        return connection;
    }
}
