package com.example.avisos.network;

import com.example.avisos.AvisoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetwork {
    private static final String VERSION = "1";

    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AvisoMod.MODID, "main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void registrar() {
        CANAL.registerMessage(0, AvisoPaquete.class, AvisoPaquete::codificar, AvisoPaquete::decodificar,
                AvisoPaquete::manejar, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CANAL.registerMessage(1, QuitarPaquete.class, QuitarPaquete::codificar, QuitarPaquete::decodificar,
                QuitarPaquete::manejar, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
