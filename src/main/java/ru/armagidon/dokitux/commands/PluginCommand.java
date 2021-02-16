package ru.armagidon.dokitux.commands;

import com.google.common.base.Joiner;
import lombok.AllArgsConstructor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.armagidon.dokitux.Dokitux;
import ru.armagidon.dokitux.pluginmanagement.PluginFile;
import ru.armagidon.dokitux.pluginmanagement.PluginFileManager;
import ru.armagidon.dokitux.utils.DownloadCallback;
import ru.armagidon.dokitux.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PluginCommand extends DokituxCommand
{

    private final PluginFileManager pluginManager;

    public PluginCommand(PluginFileManager pluginManager) {
        super("plugin");
        setUsage("&a<command> [delete [pluginname], get [name]]");
        this.pluginManager = pluginManager;
    }

    @Override
    public boolean handle(CommandSender sender, String[] args) {
        if(args.length < 2) return false;

        String command = args[0];
        if(command.equalsIgnoreCase("delete")) {
            String pluginName = args[1];
            if (pluginManager.erasePlugin(pluginName)) {
                sender.sendMessage(ChatColor.GREEN + "Plugin successfully deleted!");
                TextComponent component = new TextComponent(StringUtils.create("&e&nReload &ayour server to fully delete plugin!").color().apply());
                sender.sendMessage(component);
            } else {
                sender.sendMessage("Failed to delete plugin " + pluginName);
            }
        }
        else if(command.equalsIgnoreCase("get")) {
            if (!(sender instanceof Conversable)) return true;
            Dokitux.PLUGIN_THREAD_POOL.submit(() -> {
                String name = Joiner.on(' ').join(Arrays.copyOfRange(args, 1, args.length));
                List<PluginFile> plugins = pluginManager.findPlugin(name);
                PluginFile chosenPlugin;
                if (plugins.size() > 1) {
                    ConversationFactory factory = new ConversationFactory(pluginManager.getPlugin());
                    Conversation conversation = factory.withFirstPrompt(new PluginListPrompt(pluginManager, plugins))
                            .withModality(false)
                            .withLocalEcho(false)
                            .addConversationAbandonedListener(event -> event.getContext().getAllSessionData().put("abandoned", true))
                            .buildConversation((Conversable) sender);
                    conversation.begin();
                    return;
                } else {
                    chosenPlugin = plugins.get(0);
                }

                chosenPlugin.printInfo(sender);

                chosenPlugin.download(chosenPlugin.getId(), pluginManager, new DownloadCallback() {
                    @Override
                    public void success() {
                        sender.sendMessage(ChatColor.GREEN + "Plugin has been downloaded!");
                    }

                    @Override
                    public void fail(String message) {
                        sender.sendMessage(ChatColor.RED + "Failed to download plugin.");
                        sender.sendMessage(ChatColor.RED + message);
                    }
                });
            });

        }

        return true;
    }

    @Override
    public List<String> tabComplete(String[] args) throws IllegalArgumentException {
        if(args.length == 1){
            return Stream.of("delete","get").filter(s -> s.startsWith(args[0])).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        } else if(args.length == 2){
            return new ArrayList<>(pluginManager.getPLUGIN_FILE_REGISTRY().keySet()).stream().filter(s -> s.startsWith(args[1])).sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @AllArgsConstructor
    private static final class PluginListPrompt extends ValidatingPrompt {

        private final PluginFileManager pluginManager;
        private final List<PluginFile> plugins;
        private static final String QUIT_COMMAND = "q";

        @Override
        protected boolean isInputValid(@NotNull ConversationContext context, @NotNull String input) {
            try {
                Integer.parseInt(input);
                return true;
            } catch (NumberFormatException e) {
                if (input.equalsIgnoreCase(QUIT_COMMAND)) return true;
            }
            context.getForWhom().sendRawMessage(ChatColor.RED + "Incorrect id");
            return false;
        }

        @Nullable
        @Override
        protected Prompt acceptValidatedInput(@NotNull ConversationContext context, @NotNull String input) {
            if (input.isEmpty()) return END_OF_CONVERSATION;
            if (input.equalsIgnoreCase(QUIT_COMMAND))
                return END_OF_CONVERSATION;
            try {
                int id = Integer.parseInt(input);
                if (id > plugins.size() - 1) {
                    context.getForWhom().sendRawMessage("This id is not valid");
                    return END_OF_CONVERSATION;
                }

                PluginFile chosenPlugin = plugins.get(id);

                chosenPlugin.printInfo((CommandSender) context.getForWhom());

                chosenPlugin.download(chosenPlugin.getId(), pluginManager, new DownloadCallback() {
                    @Override
                    public void success() {
                        context.getForWhom().sendRawMessage(ChatColor.GREEN + "Plugin has been downloaded!");
                    }

                    @Override
                    public void fail(String message) {
                        context.getForWhom().sendRawMessage(ChatColor.RED + "Failed to download plugin.");
                        context.getForWhom().sendRawMessage(ChatColor.RED + message);
                    }
                });


                return END_OF_CONVERSATION;
            } catch (NumberFormatException e) {
                return END_OF_CONVERSATION;
            }
        }

        @NotNull
        @Override
        public String getPromptText(@NotNull ConversationContext context) {
            AtomicInteger integer = new AtomicInteger(0);
            return "Choose plugin to download\n"+Joiner.on('\n').join(plugins.stream().map(plugin -> integer.getAndIncrement() + plugin.getName()).collect(Collectors.toList()));
        }
    }



}
