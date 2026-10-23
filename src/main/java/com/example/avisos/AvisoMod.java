package com.example.avisos;

import com.example.avisos.network.ModNetwork;
import net.minecraftforge.fml.common.Mod;

@Mod(AvisoMod.MODID)
public class AvisoMod {
    public static final String MODID = "avisos";

    public AvisoMod() {
        ModNetwork.registrar();
    }
}
