package com.scubakay.pruned.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.storage.WebDAVStorage;
import com.scubakay.pruned.util.DynamicDialogs;
import com.scubakay.pruned.util.MachineIdentifier;
import com.scubakay.pruned.util.PasswordEncryptor;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.scubakay.pruned.command.PermissionManager.CONFIGURE_PERMISSION;
import static com.scubakay.pruned.command.PermissionManager.hasPermission;

public class WebDavLoginCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        // Subcommand to open the dialog
        ArgumentCommandNode<ServerCommandSource, String> loginNode = CommandManager.argument("endpoint", StringArgumentType.string())
            .then(CommandManager.argument("username", StringArgumentType.string())
                .then(CommandManager.argument("password", StringArgumentType.string())
                    .executes(WebDavLoginCommand::login)
                )
            ).build();

        LiteralCommandNode<ServerCommandSource> openDialogNode = CommandManager.literal("login")
            .requires(ctx -> hasPermission(ctx, CONFIGURE_PERMISSION))
            .then(CommandManager.literal("webdav")
                .executes(WebDavLoginCommand::openWebDavConfigDialog)
                .then(loginNode)
            ).build();
        Commands.getRoot(dispatcher).addChild(openDialogNode);
    }

    private static int openWebDavConfigDialog(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        try {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("%ENDPOINT%", Config.webDavEndpoint != null ? Config.webDavEndpoint : "");
            replacements.put("%USERNAME%", Config.webDavUsername != null ? Config.webDavUsername : "");

            String dialogJson = DynamicDialogs.getDialogJson("webdav_config", replacements);
            String command = String.format("dialog show @s %s", dialogJson);
            context.getSource().getDispatcher().execute(command, context.getSource());
            return 1;
        } catch (IOException e) {
            context.getSource().sendError(Text.literal("Failed to load dialog: " + e.getMessage()));
            return 0;
        }
    }

    private static int login(CommandContext<ServerCommandSource> context) {
        String endpoint = StringArgumentType.getString(context, "endpoint");
        String username = StringArgumentType.getString(context, "username");
        String password = StringArgumentType.getString(context, "password");
        String machineId = MachineIdentifier.getMachineId();
        String encryptedPassword = PasswordEncryptor.encrypt(password, machineId);
        Config.webDavEndpoint = endpoint;
        Config.webDavUsername = username;
        Config.webDavPassword = encryptedPassword;
        MidnightConfig.write(PrunedMod.MOD_ID);
        WebDAVStorage.connect(context.getSource().getServer());
        if (WebDAVStorage.isConnected()) {
            context.getSource().sendFeedback(() -> Text.literal("WebDAV credentials updated"), false);
            return 1;
        } else {
            context.getSource().sendFeedback(() -> Text.literal("Could not connect to WebDAV server"), false);
            return 0;
        }
    }
}
