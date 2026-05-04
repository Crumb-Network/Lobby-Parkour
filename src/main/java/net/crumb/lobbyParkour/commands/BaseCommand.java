package net.crumb.lobbyParkour.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.crumb.lobbyParkour.LobbyParkour;
import net.crumb.lobbyParkour.guis.LeaderboardMenu;
import net.crumb.lobbyParkour.guis.MainMenu;
import net.crumb.lobbyParkour.systems.LeaderboardUpdater;
import net.crumb.lobbyParkour.utils.MMUtils;
import net.crumb.lobbyParkour.utils.MessageType;
import net.crumb.lobbyParkour.utils.ReloadParkour;
import net.crumb.lobbyParkour.utils.SoundUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class BaseCommand {
    private final LobbyParkour plugin = LobbyParkour.getInstance();
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final LeaderboardUpdater updater = LeaderboardUpdater.getInstance();
    private static final int CENTER_PX = 130;

    LiteralArgumentBuilder<CommandSourceStack> baseCommand = Commands.literal("lpk")
            .requires(source -> source.getSender().hasPermission("ptz.admin"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (sender instanceof Player player) {
                    MainMenu.openMenu(player);
                }
                return Command.SINGLE_SUCCESS;
            })
            .then(Commands.literal("help")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        sender.sendMessage(mm.deserialize("<aqua>/lpk <gray>- Opens the main menu"));
                        sender.sendMessage(mm.deserialize("<aqua>/lpk credits <gray>- Shows a credits message"));
                        sender.sendMessage(mm.deserialize("<aqua>/lpk help <gray>- Shows a list of available commands"));
                        return Command.SINGLE_SUCCESS;
                    })
            )
            .then(Commands.literal("credits")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (sender instanceof Player player) {
                            sendCredits(player);
                        }
                        return Command.SINGLE_SUCCESS;
                    })
            )
            .then(Commands.literal("leaderboards")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (sender instanceof Player player) {
                            LeaderboardMenu.openMenu(player);
                        }
                        return Command.SINGLE_SUCCESS;
                    })
            )
            .then(Commands.literal("cache")
                    .executes(ctx -> {
                        ctx.getSource().getSender().sendMessage(updater.getCache().toString());
                        return 1;
                    })
            )
            .then(Commands.literal("test")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(ctx -> {
                                String msg = StringArgumentType.getString(ctx, "message");
                                CommandSender sender = ctx.getSource().getSender();
                                if (sender instanceof Player player) {
                                    MMUtils.sendMessage(player, msg, MessageType.NONE);
                                }
                                return 1;
                            })
                    )
            )
            .then(Commands.literal("reload")
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        if (sender instanceof Player player) {
                            ReloadParkour.reload(player);
                        }
                        return 1;
                    })
            );



    LiteralCommandNode<CommandSourceStack> buildCommand = baseCommand.build();

    public LiteralCommandNode<CommandSourceStack> getBuildCommand() {
        return buildCommand;
    }
        
    private void sendCredits(Player player) {
        List<String> lines = List.of(
                "<gradient:#d81bf5:#fa2dc3><st>                                                        </gradient>",
                "    <color:#d81bf5>⭐ <bold><gradient:#d81bf5:#fa2dc3>ʟᴏʙʙʏ ᴘᴀʀᴋᴏᴜʀ</gradient> <reset><color:#fa2dc3>⭐",
                "",
                "    <gray>Brought to you by:</gray>  <gradient:#ffa300:#ff0500>crumb",
                "    <gray>Developed by:</gray> Kalbskinder <gray>&</gray> ZetMine",
                "",
                "    <dark_gray>»</dark_gray> Join us at <dark_gray>«</dark_gray>",
                "        <dark_gray>→</dark_gray> <click:OPEN_URL:https://discord.gg/8xQXBbCa8R><hover:show_text:'<blue>Click to join!'><blue>https://discord.gg/8xQXBbCa8R    ",
                "<gradient:#d81bf5:#fa2dc3><st>                                                        </gradient>"
        );


        lines.forEach(line -> {
            MMUtils.sendMessage(player, deserializeCentered(line));
        });

        SoundUtils.playSoundSequence(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1f, 0);
        SoundUtils.playSoundSequence(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.1f, 3);
        SoundUtils.playSoundSequence(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f, 6);
        SoundUtils.playSoundSequence(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f, 9);
        SoundUtils.playSoundSequence(player, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.4f, 12);
    }

    public static String deserializeCentered(String input) {
        if (input == null || !input.startsWith("<center>")) {
            return input;
        }

        String raw = input.replaceFirst("<center>", "");
        Component component = mm.deserialize(raw);

        String plainText = PlainTextComponentSerializer.plainText().serialize(component);

        int pixelLength = getPixelLength(Component.text(plainText));
        int spaces = Math.max(0, (CENTER_PX - pixelLength / 2) / 4);

        return " ".repeat(spaces) + mm.serialize(component);
    }

    private static int getPixelLength(Component component) {
        String plain = MiniMessage.miniMessage().serialize(component).replaceAll("<[^>]+>", ""); // Ignore minimessage tags
        int length = 0;

        for (char c : plain.toCharArray()) {
            int charWidth = switch (c) {
                case 'i', 'l', '.', ',', ':' -> 2;
                case 't', 'f' -> 4;
                case 'w', 'm', 'W', 'M' -> 7;
                case ' ' -> 4;
                default -> 5;
            };
            length += charWidth + 1;
        }
        return length;
    }
}