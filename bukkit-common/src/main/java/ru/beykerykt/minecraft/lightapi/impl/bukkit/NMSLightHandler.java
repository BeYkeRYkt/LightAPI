package ru.beykerykt.minecraft.lightapi.impl.bukkit;

import ru.beykerykt.minecraft.lightapi.common.ImplementationPlatform;
import ru.beykerykt.minecraft.lightapi.common.MappingType;

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

	@Override
	public MappingType getMappingType() {
		return MappingType.CB;
	}
}
