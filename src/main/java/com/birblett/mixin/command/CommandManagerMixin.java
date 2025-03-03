package com.birblett.mixin.command;

import com.birblett.TechnicalToolbox;
import com.birblett.impl.command.CameraCommand;
import com.birblett.impl.command.ToolboxCommand;
import com.birblett.impl.command.alias.AliasCommand;
import com.birblett.impl.command.alias.AliasManager;
import com.birblett.impl.command.alias.AliasedCommand;
import com.birblett.impl.command.delay.DelayCommand;
import com.birblett.impl.command.stat.StatCommand;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers some custom commands. Aliases and camera command are registered on-demand as well.
 */
@Mixin(CommandManager.class)
public class CommandManagerMixin {

    @Shadow @Final private CommandDispatcher<ServerCommandSource> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onRegister(CommandManager.RegistrationEnvironment environment, CommandRegistryAccess commandRegistryAccess, CallbackInfo ci) {
        ToolboxCommand.register(this.dispatcher);
        AliasCommand.register(this.dispatcher);
        CameraCommand.register(this.dispatcher);
        DelayCommand.register(this.dispatcher);
        StatCommand.register(this.dispatcher, commandRegistryAccess);
        for (AliasedCommand aliasedCommand : AliasManager.ALIASES.values()) {
            try {
                aliasedCommand.register(this.dispatcher);
            }
            catch (Exception e) {
                TechnicalToolbox.error("Something went wrong with compiling alias {}", aliasedCommand.getAlias());
            }
        }
    }

}