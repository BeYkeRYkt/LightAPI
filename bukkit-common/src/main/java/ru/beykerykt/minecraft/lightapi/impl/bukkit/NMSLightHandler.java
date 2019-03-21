package ru.beykerykt.minecraft.lightapi.impl.bukkit;

import ru.beykerykt.minecraft.lightapi.common.ImplementationPlatform;

/**
 * 
 * Interface implementation for NMS (Net Minecraft Server)
 * 
 * @author BeYkeRYkt
 *
 */
public abstract class NMSLightHandler implements IBukkitLightHandler {

	@Override
	public ImplementationPlatform getImplementationPlatform() {
		return ImplementationPlatform.CRAFTBUKKIT;
	}
}
