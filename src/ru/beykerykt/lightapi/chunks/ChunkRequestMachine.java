package ru.beykerykt.lightapi.chunks;

import ru.beykerykt.lightapi.light.LightDataRequest;
import ru.beykerykt.lightapi.nms.NMSHelper;
import ru.beykerykt.lightapi.request.DataRequest;
import ru.beykerykt.lightapi.request.RequestSteamMachine;

public class ChunkRequestMachine extends RequestSteamMachine {

	@Override
	public void run() {
		super.run();

		while (!ChunkCache.CHUNK_INFO_QUEUE.isEmpty()) {
			ChunkInfo info = ChunkCache.CHUNK_INFO_QUEUE.get(0);
			NMSHelper.sendChunkUpdate(info.getWorld(), info.getX(), info.getZ(), info.getReceivers());
			ChunkCache.CHUNK_INFO_QUEUE.remove(0);
		}
	}

	@Override
	public boolean process(DataRequest request) {
		if (!request.isReadyForSend()) {
			return false;
		}
		if (request instanceof LightDataRequest) {
			LightDataRequest r = (LightDataRequest) request;
			int chunkX = r.getX() >> 4;
			int chunkZ = r.getZ() >> 4;
			for (ChunkInfo info : NMSHelper.collectChunks(r.getWorld(), chunkX, chunkZ)) {
				if (ChunkCache.CHUNK_INFO_QUEUE.contains(info)) {
					int index = ChunkCache.CHUNK_INFO_QUEUE.indexOf(info);
					ChunkCache.CHUNK_INFO_QUEUE.remove(index);
				}
				ChunkCache.CHUNK_INFO_QUEUE.add(info);
			}
			return true;
		}
		return false;
	}
}
