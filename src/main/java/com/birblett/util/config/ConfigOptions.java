package com.birblett.util.config;

import com.birblett.TechnicalToolbox;
import com.birblett.command.CameraCommand;
import com.birblett.command.ToolboxCommand;
import com.birblett.lib.NodeRemovalInterface;
import com.birblett.util.ServerUtil;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

public enum ConfigOptions implements ConfigOption<Object> {
    CAMERA_COMMAND("cameraCommand", "cam", "Camera command string, usage /[cmd string] i.e. " +
            "/cam. Default: cam", "cam", "c", "cs") {
        private String value = "cam";

        @Override
        public String value() {
            return this.value;
        }

        @Override
        public String setFromString(String value, MinecraftServer server) {
            String oldValue = this.value;
            String s = setFromString(value);
            if (s == null && server != null) {
                ServerUtil.removeCommandByName(server, oldValue);
                CameraCommand.register(server.getCommandManager().getDispatcher());
            }
            return s;
        }

        @Override
        public String setFromString(String value) {
            Pair<String, String> out = ConfigHelper.getStringOption(this.getName(), value, "cam");
            this.value = out.getLeft();
            return out.getRight();
        }
    },
    CAMERA_PERMISSION_LEVEL("cameraPermissionLevel", "0", "Permission level required to use the" +
            "camera command. Default: 0", "0", "2", "4") {
        private int value = 0;

        @Override
        public Integer value() {
            return this.value;
        }

        @Override
        public String setFromString(String value) {
            Pair<Integer, String> out = ConfigHelper.getIntOption(this.getName(), value, 4, 0, 4);
            this.value = out.getLeft();
            return out.getRight();
        }
    },
    CRAFTER_COOLDOWN("crafterCooldown", "4", "0", "4") {
        private int value = 4;

        @Override
        public Integer value() {
            return this.value;
        }

        @Override
        public String getDesc() {
            return "Gameticks of crafter block cooldown, will be instant if set to 0. Default: 4";
        }

        @Override
        public String setFromString(String value) {
            Pair<Integer, String> out = ConfigHelper.getIntOption(this.getName(), value, 4, 0, Integer.MAX_VALUE);
            this.value = out.getLeft();
            return out.getRight();
        }
    },
    CRAFTER_QUASI_POWER("crafterQuasiPower", "false", "true", "false") {
        private boolean value = false;

        @Override
        public Boolean value() {
            return this.value;
        }

        @Override
        public String getDesc() {
            return "Whether crafter droppers can be quasi-powered or not. Default: false";
        }

        @Override
        public String setFromString(String value) {
            Pair<Boolean, String> out = ConfigHelper.getBooleanOption(this.getName(), value, false);
            this.value = out.getLeft();
            return out.getRight();
        }
    },
    CRAFTER_DISABLED_SLOT_ITEMS("crafterDisabledSlotItems", "minecraft:wooden_shovel", "minecraft:wooden_shovel") {
        private String writeableOption = "items:wooden_shovel";
        private Predicate<ItemStack> value = stack -> stack.isOf(Items.WOODEN_SHOVEL);

        @Override
        public Predicate<ItemStack> value() {
            return this.value;
        }

        @Override
        public String getDesc() {
            return "Items to act as disabled slots in dropper crafters; accepts item id, specific custom " +
                    "names, or nbt tag(s) to match. Default: minecraft:wooden_shovel";
        }

        @Override
        public String setFromString(String value) {
            Identifier identifier = Identifier.tryParse(value);
            if (identifier != null) {
                if (!Registries.ITEM.containsId(identifier)) {
                    return "Invalid item id '" + value + "' for option " + this.getName();
                }
                Item item = Registries.ITEM.get(identifier);
                this.value = stack -> stack.isOf(item);
                this.writeableOption = value;
                return null;
            }
            try {
                NbtCompound nbt = StringNbtReader.parse(value);
                this.value = stack -> {
                    NbtCompound compound = stack.getNbt();
                    if (compound != null) {
                        for (String key : nbt.getKeys()) {
                            if (!compound.contains(key)) {
                                return false;
                            }
                            NbtElement e1 = compound.get(key), e2 = nbt.get(key);
                            if (e1 == null || e2 == null || !e1.toString().equals(e2.toString())) {
                                return false;
                            }
                        }
                        return true;
                    }
                    return false;
                };
                this.writeableOption = value;
                return null;
            }
            catch (CommandSyntaxException e) {
                if (value.length() >= 3 && value.endsWith("\"") && value.startsWith("\"")) {
                    String s = value.substring(1, value.length() - 1);
                    this.value = stack -> stack.getName().getString().equals(s);
                    this.writeableOption = value;
                    return null;
                }
                return "Did not read valid item identifier, nbt, or custom name from value '" + value + "' for " +
                        "option " + this.getName();
            }
        }

        @Override
        public String getWriteable() {
            return this.writeableOption;
        }
    },
    ;

    private final String name;
    private final String desc;
    private final String defaultValue;
    private final Collection<String> commandSuggestions;

    ConfigOptions(String name, String defaultValue, String desc, String... suggestions) {
        this.name = name;
        this.desc = desc;
        this.defaultValue = defaultValue;
        this.commandSuggestions = Arrays.asList(suggestions);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDesc() {
        return this.desc;
    }

    @Override
    public String getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public Collection<String> commandSuggestions() {
        return this.commandSuggestions;
    }

}
