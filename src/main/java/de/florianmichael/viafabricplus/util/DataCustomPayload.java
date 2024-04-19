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

package de.florianmichael.viafabricplus.util;

import de.florianmichael.viafabricplus.fixes.ClientsideFixes;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DataCustomPayload(PacketByteBuf buf) implements CustomPayload {

    public static final CustomPayload.Id<DataCustomPayload> ID = new CustomPayload.Id<>(new Identifier(ClientsideFixes.PACKET_SYNC_IDENTIFIER));

    public static void init() {
        PayloadTypeRegistry.playS2C().register(DataCustomPayload.ID, CustomPayload.codecOf((value, buf) -> {
            throw new UnsupportedOperationException("DataCustomPayload is a read-only packet");
        }, buf -> new DataCustomPayload(new PacketByteBuf(Unpooled.copiedBuffer(buf.readSlice(buf.readableBytes()))))));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

}
