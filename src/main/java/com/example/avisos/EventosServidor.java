package com.example.avisos;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = AvisoMod.MODID)
public class EventosServidor {

    /** Al entrar, el jugador recibe los avisos que están fijos en pantalla. */
    @SubscribeEvent
    public static void alEntrar(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer jugador)) return;
        MinecraftServer server = jugador.getServer();
        if (server == null) return;

        AvisoDatos datos = AvisoDatos.get(server);
        for (Map.Entry<String, AvisoDatos.Permanente> entrada : datos.permanentes.entrySet()) {
            AvisoDatos.Permanente p = entrada.getValue();
            if (!p.todos && !p.jugadores.contains(jugador.getUUID())) continue;
            Aviso aviso = datos.avisos.get(entrada.getKey());
            if (aviso == null) continue;
            Servicio.enviar(List.of(jugador), entrada.getKey(), aviso);
        }
    }
}
