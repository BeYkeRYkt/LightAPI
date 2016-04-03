package ru.beykerykt.lightapi.light;

import org.bukkit.block.Block;

import ru.beykerykt.lightapi.nms.NMSHelper;
import ru.beykerykt.lightapi.request.DataRequest;
import ru.beykerykt.lightapi.request.RequestSteamMachine;

public class LightRequestMachine extends RequestSteamMachine {

	public synchronized boolean process(LightDataRequest request) {
		if (!request.isReadyForSend()) {
			switch (request.getRequestType()) {
				case CREATE:
					NMSHelper.createLight(request.getWorld(), request.getX(), request.getY(), request.getZ(), request.getLightLevel());
					break;
				case DELETE:
					NMSHelper.deleteLight(request.getWorld(), request.getX(), request.getY(), request.getZ());
					break;
				case RECALCULATE:
					NMSHelper.recalculateLight(request.getWorld(), request.getX(), request.getY(), request.getZ());
					break;
				case CREATE_AND_RECALCULATE:
					NMSHelper.createLight(request.getWorld(), request.getX(), request.getY(), request.getZ(), request.getLightLevel());
					Block adjacent = Lights.getAdjacentAirBlock(request.getWorld().getBlockAt(request.getX(), request.getY(), request.getZ()));
					int lx = adjacent.getX();
					int ly = adjacent.getY();
					int lz = adjacent.getZ();
					NMSHelper.recalculateLight(request.getWorld(), lx, ly, lz);
					break;
			}
			request.setReadyForSend(true);
		}
		return true;
	}

	@Override
	public synchronized boolean process(DataRequest request) {
		return process((LightDataRequest) request);
	}
}
