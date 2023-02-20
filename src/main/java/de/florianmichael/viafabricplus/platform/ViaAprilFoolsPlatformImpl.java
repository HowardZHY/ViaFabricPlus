/**
 * --FLORIAN MICHAEL PRIVATE LICENCE v1.2--
 *
 * This file / project is protected and is the intellectual property of Florian Michael (aka. EnZaXD),
 * any use (be it private or public, be it copying or using for own use, be it publishing or modifying) of this
 * file / project is prohibited. It requires in that use a written permission with official signature of the owner
 * "Florian Michael". "Florian Michael" receives the right to control and manage this file / project. This right is not
 * cancelled by copying or removing the license and in case of violation a criminal consequence is to be expected.
 * The owner "Florian Michael" is free to change this license. The creator assumes no responsibility for any infringements
 * that have arisen, are arising or will arise from this project / file. If this licence is used anywhere,
 * the latest version published by the author Florian Michael (aka EnZaXD) always applies automatically.
 *
 * Changelog:
 *     v1.0:
 *         Added License
 *     v1.1:
 *         Ownership withdrawn
 *     v1.2:
 *         Version-independent validity and automatic renewal
 */

package de.florianmichael.viafabricplus.platform;

import com.viaversion.viaversion.api.Via;
import de.florianmichael.vialoadingbase.util.JLoggerToLog4j;
import net.raphimc.vialegacy.platform.ViaLegacyPlatform;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.util.logging.Logger;

public class ViaAprilFoolsPlatformImpl implements ViaLegacyPlatform {
    private static final Logger LOGGER = new JLoggerToLog4j(LogManager.getLogger("ViaAprilFools"));

    public ViaAprilFoolsPlatformImpl() {
        this.init(this.getDataFolder());
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public File getDataFolder() {
        return Via.getPlatform().getDataFolder();
    }
}