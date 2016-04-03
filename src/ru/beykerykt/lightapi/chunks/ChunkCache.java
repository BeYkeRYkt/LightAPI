package ru.beykerykt.lightapi.chunks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChunkCache {

	public static List<ChunkInfo> CHUNK_INFO_QUEUE = new CopyOnWriteArrayList<ChunkInfo>();

}
