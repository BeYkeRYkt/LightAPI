package ru.beykerykt.lightapi.chunks;

import ru.beykerykt.lightapi.light.LightDataRequest;
import ru.beykerykt.lightapi.nms.NMSHelper;
import ru.beykerykt.lightapi.request.DataRequest;
import ru.beykerykt.lightapi.request.RequestSteamMachine;

public class ChunkRequestMachine extends RequestSteamMachine {

	@Override
	public void run() {
		super.run();
		ChunkCache.sendChunkUpdates();
	}

	@Override
	public synchronized boolean process(DataRequest request) {
		if (!request.isReady()) {
			return false;
		}
		LightDataRequest r = (LightDataRequest) request;
		int chunkX = r.getX() >> 4;
		int chunkZ = r.getZ() >> 4;
		for (ChunkInfo info : NMSHelper.collectChunks(r.getWorld(), chunkX, chunkZ)) {
			if (!ChunkCache.CHUNK_INFO_CACHE.contains(info)) {
				ChunkCache.CHUNK_INFO_CACHE.add(info);
			}
		}
		return true;
	}
}
