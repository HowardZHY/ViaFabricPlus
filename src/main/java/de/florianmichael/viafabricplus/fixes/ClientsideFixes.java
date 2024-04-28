/*
 * This file is part of ViaFabricPlus - https://github.com/FlorianMichael/ViaFabricPlus
 * Copyright (C) 2021-2024 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and RK_01/RaphiMC
 * Copyright (C) 2023-2024 contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianmichael.viafabricplus.fixes;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.event.ChangeProtocolVersionCallback;
import de.florianmichael.viafabricplus.event.PostGameLoadCallback;
import de.florianmichael.viafabricplus.fixes.data.EntityDimensionDiff;
import de.florianmichael.viafabricplus.fixes.data.ResourcePackHeaderDiff;
import de.florianmichael.viafabricplus.fixes.versioned.classic.CPEAdditions;
import de.florianmichael.viafabricplus.fixes.versioned.classic.GridItemSelectionScreen;
import de.florianmichael.viafabricplus.fixes.versioned.visual.ArmorHudEmulation1_8;
import de.florianmichael.viafabricplus.fixes.versioned.visual.FootStepParticle1_12_2;
import de.florianmichael.viafabricplus.injection.access.IClientConnection;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.BedrockSettings;
import de.florianmichael.viafabricplus.util.DataCustomPayload;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.Registries;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.viabedrock.protocol.data.ProtocolConstants;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocols.classic.protocolc0_28_30toc0_28_30cpe.data.ClassicProtocolExtension;
import net.raphimc.vialegacy.protocols.classic.protocolc0_28_30toc0_28_30cpe.storage.ExtensionProtocolMetadataStorage;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * This class contains random fields and methods that are used to fix bugs on the client side
 */
public class ClientsideFixes {

    /**
     * Contains all tasks that are waiting for a packet to be received, this system can be used to sync ViaVersion tasks with the correct thread
     */
    private static final Map<String, Consumer<RegistryByteBuf>> PENDING_EXECUTION_TASKS = new ConcurrentHashMap<>();

    /**
     * This identifier is an internal identifier that is used to identify packets that are sent by ViaFabricPlus
     */
    public static final String PACKET_SYNC_IDENTIFIER = UUID.randomUUID() + ":" + UUID.randomUUID();

    /**
     * This identifier is an internal identifier used to store the item count in <= 1.10 to implement negative item counts
     */
    public static final String ITEM_COUNT_FIX_KEY = "VFP|ItemCountFix";

    /**
     * This identifier is used to store attributes in legacy versions were we replace them using data components
     */
    public static final String ATTRIBUTE_FIX_KEY = "VFP|AttributeFix";

    public static void init() {
        // Register additional CPE features
        CPEAdditions.modifyMappings();

        // Check if the pack format mappings are correct
        ResourcePackHeaderDiff.checkOutdated();

        PostGameLoadCallback.EVENT.register(() -> {
            // Handles and updates entity dimension changes in <= 1.17
            EntityDimensionDiff.init();

            // Ticks the armor hud manually in <= 1.8.x
            ArmorHudEmulation1_8.init();
        });

        // Reloads some clientside stuff when the protocol version changes
        ChangeProtocolVersionCallback.EVENT.register((oldVersion, newVersion) -> MinecraftClient.getInstance().execute(() -> {
            // Clear all font caches to enforce a reload of all fonts (this is needed because we change the font renderer behavior)
            for (FontStorage storage : MinecraftClient.getInstance().fontManager.fontStorages.values()) {
                storage.glyphRendererCache.clear();
                storage.glyphCache.clear();
            }

            // Reloads all bounding boxes of the blocks that we changed
            for (Block block : Registries.BLOCK) {
                if (block instanceof AnvilBlock || block instanceof BedBlock || block instanceof BrewingStandBlock
                        || block instanceof CarpetBlock || block instanceof CauldronBlock || block instanceof ChestBlock
                        || block instanceof EnderChestBlock || block instanceof EndPortalBlock || block instanceof EndPortalFrameBlock
                        || block instanceof FarmlandBlock || block instanceof FenceBlock || block instanceof FenceGateBlock
                        || block instanceof HopperBlock || block instanceof LadderBlock || block instanceof LeavesBlock
                        || block instanceof LilyPadBlock || block instanceof PaneBlock || block instanceof PistonBlock
                        || block instanceof PistonHeadBlock || block instanceof SnowBlock || block instanceof WallBlock
                        || block instanceof CropBlock || block instanceof FlowerbedBlock
                ) {
                    for (BlockState state : block.getStateManager().getStates()) {
                        state.initShapeCache();
                    }
                }
            }

            // Rebuilds the item selection screen grid
            if (newVersion.olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
                GridItemSelectionScreen.INSTANCE.itemGrid = null;
            }
        }));

        // Register the footstep particle
        FootStepParticle1_12_2.init();

        // Register the custom payload packet for sync tasks
        DataCustomPayload.init();
    }

    /**
     * Calculates the maximum chat length for the selected protocol version in {@link ProtocolTranslator#getTargetVersion()}
     *
     * @return The maximum chat length
     */
    public static int getChatLength() {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.c0_28toc0_30)) {
            final ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
            final ExtensionProtocolMetadataStorage extensionProtocol = ((IClientConnection) handler.getConnection()).viaFabricPlus$getUserConnection().get(ExtensionProtocolMetadataStorage.class);

            if (extensionProtocol != null && extensionProtocol.hasServerExtension(ClassicProtocolExtension.LONGER_MESSAGES)) {
                return Short.MAX_VALUE * 2;
            } else {
                return 64 - (MinecraftClient.getInstance().getSession().getUsername().length() + 2);
            }
        } else if (ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            return 512;
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_9_3)) {
            return 100;
        } else {
            return 256;
        }
    }

    /**
     * Replaces the default port when parsing a server address if the default port should be replaced
     *
     * @param address The original address of the server
     * @param version The protocol version
     * @return The server address with the replaced default port
     */
    public static ServerAddress replaceDefaultPort(final String address, final ProtocolVersion version) {
        // If the default port for this entry should be replaced, check if the address already contains a port
        // We can't just replace vanilla's default port because a bedrock server might be running on the same port
        if (BedrockSettings.global().replaceDefaultPort.getValue() && Objects.equals(version, BedrockProtocolVersion.bedrockLatest) && !address.contains(":")) {
            return ServerAddress.parse(address + ":" + ProtocolConstants.BEDROCK_DEFAULT_PORT);
        } else {
            return ServerAddress.parse(address);
        }
    }

    /**
     * Executes a sync task and returns the uuid of the task
     *
     * @param task The task to execute
     * @return The uuid of the task
     */
    public static String executeSyncTask(final Consumer<RegistryByteBuf> task) {
        final String uuid = UUID.randomUUID().toString();
        PENDING_EXECUTION_TASKS.put(uuid, task);
        return uuid;
    }

    public static void handleSyncTask(final PacketByteBuf buf) {
        final String uuid = buf.readString();

        if (PENDING_EXECUTION_TASKS.containsKey(uuid)) {
            MinecraftClient.getInstance().execute(() -> { // Execute the task on the main thread
                final var task = PENDING_EXECUTION_TASKS.remove(uuid);
                task.accept(new RegistryByteBuf(buf, MinecraftClient.getInstance().getNetworkHandler().getRegistryManager()));
            });
        }
    }

}
