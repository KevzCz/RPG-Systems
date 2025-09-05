package net.pixeldreamstudios.rpgsystems.title.power;

import net.pixeldreamstudios.rpgsystems.title.power.powers.IlluminatePower;

public class PowerInit {
    private PowerInit() {}
    public static void init() {
        PowerRegistry.register(new IlluminatePower());
        PowerRegistry.registerServerTick();
    }
}
