/**
 * The MIT License (MIT)
 * 
 * Copyright (c) 2020 Vladimir Mikhailov <beykerykt@gmail.com>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *  
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ru.beykerykt.minecraft.lightapi.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ru.beykerykt.minecraft.lightapi.bukkit.impl.IBukkitHandlerImpl;
import ru.beykerykt.minecraft.lightapi.common.IChunkSectionsData;
import ru.beykerykt.minecraft.lightapi.common.LightAPI;

public class ChunkSenderExecutorService implements Runnable {

	private ScheduledFuture<?> sch;
	private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private Queue<Runnable> REQUEST_QUEUE = new ConcurrentLinkedQueue<Runnable>();
	private List<IChunkSectionsData> chunksToUpdate = new ArrayList<IChunkSectionsData>();
	private List<IChunkSectionsData> chunksToSend = new ArrayList<IChunkSectionsData>();
	private boolean mergeSections;

	public void start(int delayTicks, boolean mergeChunkSections) {
		sch = executor.scheduleWithFixedDelay(this, 0, 50 * delayTicks, TimeUnit.MILLISECONDS);
		this.mergeSections = mergeChunkSections;
	}

	public void shutdown() {
		REQUEST_QUEUE.clear();
		chunksToUpdate.clear();
		if (sch != null) {
			sch.cancel(false);
		}
		executor.shutdownNow();
	}

	public boolean isStarted() {
		if (executor == null || executor.isShutdown()) {
			return false;
		}
		return true;
	}

	@Override
	public void run() {
		try {
			// queue
			Runnable request;
			while ((request = REQUEST_QUEUE.poll()) != null) {
				request.run();
			}

			// merge chunk sections and sends to players
			for (int i = 0; i < chunksToUpdate.size(); i++) {
				IChunkSectionsData queueChunkData = chunksToUpdate.get(i);
				if (!mergeSections) {
					chunksToSend.add(queueChunkData);
					continue;
				}

				boolean found = false;
				for (int j = 0; j < chunksToSend.size(); j++) {
					IChunkSectionsData readyChunkData = chunksToSend.get(j);

					if (queueChunkData.getWorldName().equals(readyChunkData.getWorldName())
							&& queueChunkData.getChunkX() == readyChunkData.getChunkX()
							&& queueChunkData.getChunkZ() == readyChunkData.getChunkZ()) {
						found = true;
						readyChunkData.addSectionMaskSky(queueChunkData.getSectionMaskSky());
						readyChunkData.addSectionMaskBlock(queueChunkData.getSectionMaskBlock());
						break;
					}
				}

				if (!found) {
					chunksToSend.add(queueChunkData);
				}
			}

			for (int i = 0; i < chunksToSend.size(); i++) {
				IChunkSectionsData chunkData = chunksToSend.get(i);
				IBukkitHandlerImpl impl = (IBukkitHandlerImpl) LightAPI.get().getPluginImpl().getHandlerImpl();
				if (chunkData.getSectionMaskSky() == BukkitChunkSectionsData.FULL_MASK
						&& chunkData.getSectionMaskBlock() == BukkitChunkSectionsData.FULL_MASK) {
					impl.sendChanges(chunkData.getWorldName(), chunkData.getChunkX(), chunkData.getChunkZ());
				} else {
					impl.sendChanges(chunkData.getWorldName(), chunkData.getChunkX(), chunkData.getChunkZ(),
							chunkData.getSectionMaskSky(), chunkData.getSectionMaskBlock());
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			chunksToUpdate.clear();
			chunksToSend.clear();
		}
	}

	protected void addToQueue(Runnable request) {
		if (request == null)
			return;
		REQUEST_QUEUE.add(request);
	}

	public void addChunkToUpdate(IChunkSectionsData data) {
		if (data == null) {
			return;
		}
		addToQueue(new Runnable() {

			@Override
			public void run() {
				if (!chunksToUpdate.contains(data)) {
					chunksToUpdate.add(data);
				}
			}
		});
	}
}
