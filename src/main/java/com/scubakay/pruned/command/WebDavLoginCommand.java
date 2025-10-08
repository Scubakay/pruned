package com.scubakay.pruned.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.scubakay.pruned.PrunedMod;
import com.scubakay.pruned.config.Config;
import com.scubakay.pruned.storage.WebDAVStorage;
import com.scubakay.pruned.util.MachineIdentifier;
import com.scubakay.pruned.util.PasswordEncryptor;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class WebDavLoginCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess ignoredRegistry, CommandManager.RegistrationEnvironment ignoredEnvironment) {
        LiteralCommandNode<ServerCommandSource> loginNode = CommandManager.literal("login")
            .then(CommandManager.literal("webdav")
                .then(CommandManager.argument("endpoint", StringArgumentType.string())
                    .then(CommandManager.argument("username", StringArgumentType.string())
                        .then(CommandManager.argument("password", StringArgumentType.string())
                            .executes(WebDavLoginCommand::login)
                        )
                    )
                )
            )
            .build();
        Commands.getRoot(dispatcher).addChild(loginNode);
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
        WebDAVStorage.reload();
        context.getSource().sendFeedback(() -> Text.literal("WebDAV credentials updated securely."), false);
        return 1;
    }
}
