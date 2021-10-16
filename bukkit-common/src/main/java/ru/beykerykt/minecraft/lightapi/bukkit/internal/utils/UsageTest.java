package ru.beykerykt.minecraft.lightapi.bukkit.internal.utils;

import ru.beykerykt.minecraft.lightapi.common.LightAPI;
import ru.beykerykt.minecraft.lightapi.common.api.ResultCode;
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightType;
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy;
import ru.beykerykt.minecraft.lightapi.common.api.engine.sched.ICallback;
import ru.beykerykt.minecraft.lightapi.common.internal.engine.sched.RequestFlag;

public class UsageTest {

    private void setLightLevel(String world, int blockX, int blockY, int blockZ, int lightLevel) {
        int lightTypeFlags = LightType.BLOCK_LIGHTING;
        int oldBlockLight = LightAPI.get().getLightLevel(world, blockX, blockY, blockZ, lightTypeFlags);
        EditPolicy editPolicy = EditPolicy.DEFERRED;
        SendPolicy sendPolicy = SendPolicy.DEFERRED;
        LightAPI.get().setLightLevel(world, blockX, blockY, blockZ, lightLevel, lightTypeFlags, editPolicy, sendPolicy,
                new ICallback() {
                    @Override
                    public void onResult(int requestFlag, int resultCode) {
                        switch (requestFlag) {
                            case RequestFlag.EDIT:
                                if (resultCode == ResultCode.SUCCESS) {
                                    System.out.println("Light level was edited.");
                                } else {
                                    System.out.println("request (" + requestFlag + "): result code " + resultCode);
                                }
                                break;
                            case RequestFlag.RECALCULATE:
                                if (resultCode == ResultCode.SUCCESS) {
                                    int newBlockLight = LightAPI.get().getLightLevel(world, blockX, blockY, blockZ,
                                            lightTypeFlags);
                                    if (oldBlockLight != newBlockLight) {
                                        System.out.println("Light level was recalculated.");
                                    } else {
                                        System.out.println("Light level was not recalculated.");
                                    }
                                } else {
                                    System.out.println("request (" + requestFlag + "): result code " + resultCode);
                                }
                                break;
                            case RequestFlag.SEPARATE_SEND:
                                System.out.println("request (" + requestFlag + "): result code " + resultCode);
                                break;
                        }
                    }
                });
    }
}
