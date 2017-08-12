package ru.beykerykt.android.experimental.threadhotplug;

import ru.beykerykt.android.experimental.threadhotplug.hotplugs.DynamicHotplug;

public class Tester {

	public static void main(String[] args) {
		DynamicHotplug hotplug = new DynamicHotplug();
		hotplug.setMaxThreads(4 * 2);
		hotplug.setMaxQueueSize(500);

		TaskScheduler.setHotplug(hotplug);
		TaskScheduler.start();

		System.out.println("MaxThreads: " + TaskScheduler.getHotplug().getMaxThreads());
		System.out.println("CurrentThreads: " + TaskScheduler.getHotplug().getCurrentThreads());
		try {
			for (int i = 0; i < 1000; i++) {
				TaskScheduler.execute(new Task() {
					@Override
					public void process() {
						System.out.println("TEST");
						System.out.println("Current:" + TaskScheduler.getHotplug().getCurrentThreads());
					}

					@Override
					public boolean isRepeatable() {
						// TODO Auto-generated method stub
						return false;
					}

					@Override
					public int getPriority() {
						return 0;
					}
				});
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		try {
			Thread.sleep(2000);
			System.out.println("Current Cores: " + TaskScheduler.getHotplug().getCurrentThreads());
			TaskScheduler.shutdown();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
