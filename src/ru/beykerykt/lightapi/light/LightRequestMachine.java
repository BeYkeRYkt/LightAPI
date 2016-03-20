package ru.beykerykt.lightapi.light;

import ru.beykerykt.lightapi.nms.NMSHelper;
import ru.beykerykt.lightapi.request.DataRequest;
import ru.beykerykt.lightapi.request.RequestSteamMachine;

public class LightRequestMachine extends RequestSteamMachine {

	public synchronized boolean process(LightDataRequest request) {
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
		}
		request.setReady(true);
		// if (request.getChildrenRequest() != null) {
		// return process(request.getChildrenRequest());
		// }
		return true;
	}

	@Override
	public synchronized boolean process(DataRequest request) {
		return process((LightDataRequest) request);
	}
}
