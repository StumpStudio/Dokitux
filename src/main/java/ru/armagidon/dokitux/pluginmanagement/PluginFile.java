package ru.armagidon.dokitux.pluginmanagement;

import com.google.gson.*;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import ru.armagidon.dokitux.Dokitux;
import ru.armagidon.dokitux.utils.DownloadCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.stream.StreamSupport;

import static ru.armagidon.dokitux.pluginmanagement.PluginFileManager.makeConnection;

@Data
@Builder
@ToString
public class PluginFile
{

    public static final PluginFileSerializer SERIALIZER = new PluginFileSerializer();

    private final int id;
    private String name;
    private final String author;
    private final String version;
    private final String[] supportedVersions;
    private File file;

    public void download(int id, PluginFileManager manager, DownloadCallback callback) {
        Dokitux.PLUGIN_THREAD_POOL.submit(() -> {
            HttpURLConnection downloadLinkRequest = null;
            try {
                downloadLinkRequest = makeConnection("http://api.spiget.org/v2/resources/" + id + "/download");

                if (downloadLinkRequest.getHeaderField("Content-Disposition") == null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(downloadLinkRequest.getInputStream()));

                    String link = reader.readLine().split(" ")[4];
                    downloadLinkRequest = makeConnection(link);

                    reader.close();
                }

                final String rawName = downloadLinkRequest.getHeaderField("Content-Disposition").split("=\"")[1];
                String fileName = rawName.substring(0, rawName.lastIndexOf("#")) + rawName.substring(rawName.lastIndexOf('.'));
                fileName = fileName.substring(0, fileName.length() - 1);

                File dest = new File(manager.getPluginsFolder(), fileName);

                ReadableByteChannel channel = Channels.newChannel(downloadLinkRequest.getInputStream());
                FileOutputStream stream = new FileOutputStream(dest);
                FileChannel fileChannel = stream.getChannel();
                fileChannel.transferFrom(channel, 0, Long.MAX_VALUE);

                this.file = dest;

                PluginDescriptionFile descriptionFile = manager.getPlugin().getPluginLoader().getPluginDescription(dest);
                Plugin newPlugin = manager.getPlugin().getPluginLoader().loadPlugin(dest);
                Bukkit.getScheduler().runTask(manager.getPlugin(), () -> manager.getPlugin().getPluginLoader().enablePlugin(newPlugin));
                this.name = descriptionFile.getName();
                manager.putPluginToAccount(descriptionFile, this);

                callback.success();

            } catch (Exception e) {
                callback.fail(e.getMessage());
            } finally {
                downloadLinkRequest.disconnect();
            }
        });
    }

    public void printInfo(CommandSender who) {
        who.sendMessage("Name: " + name);
        who.sendMessage("Author: " + author);
        who.sendMessage("Version: " + version);
        who.sendMessage("Supported versions: "+ Arrays.toString(supportedVersions));
    }

    public static class PluginFileSerializer implements JsonSerializer<PluginFile>, JsonDeserializer<PluginFile> {

        @Override
        public JsonElement serialize(PluginFile src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", src.getId());
            jsonObject.addProperty("name", src.getName());
            jsonObject.addProperty("version", src.getVersion());
            jsonObject.addProperty("author", src.getAuthor());
            jsonObject.addProperty("file", src.getFile().getAbsolutePath());

            JsonArray supportedVersions = new JsonArray();
            for (String supportedVersion : src.getSupportedVersions()) {
                supportedVersions.add(supportedVersion);
            }
            jsonObject.add("supportedVersions", supportedVersions);

            return jsonObject;
        }

        @Override
        public PluginFile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject pluginFile = json.getAsJsonObject();

            return PluginFile.builder().author(pluginFile.get("author").getAsString())
                    .id(pluginFile.get("id").getAsInt())
                    .name(pluginFile.get("name").getAsString())
                    .version(pluginFile.get("version").getAsString())
                    .supportedVersions(StreamSupport.stream(pluginFile.get("supportedVersions").getAsJsonArray().spliterator(), false).
                            map(JsonElement::getAsString).toArray(String[]::new)).file(new File(pluginFile.get("file").getAsString()))
                    .build();
        }
    }

}
