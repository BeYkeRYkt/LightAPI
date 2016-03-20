package ru.beykerykt.lightapi.chunks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ru.beykerykt.lightapi.nms.NMSHelper;

public class ChunkCache {

	public static List<ChunkInfo> CHUNK_INFO_CACHE = new CopyOnWriteArrayList<ChunkInfo>();

	public static void sendChunkUpdates() {
		while (!ChunkCache.CHUNK_INFO_CACHE.isEmpty()) {
			ChunkInfo info = ChunkCache.CHUNK_INFO_CACHE.get(0);
			NMSHelper.sendChunkUpdate(info);
			ChunkCache.CHUNK_INFO_CACHE.remove(0);
		}
	}
}
