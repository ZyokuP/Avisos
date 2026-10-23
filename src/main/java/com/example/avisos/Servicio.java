package com.example.avisos;

import com.example.avisos.network.AvisoPaquete;
import com.example.avisos.network.ModNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Lógica del servidor: a quién se le muestra qué aviso, y cuáles quedan guardados como fijos. */
public final class Servicio {
    private Servicio() {}

    public static List<ServerPlayer> destinatarios(MinecraftServer server, boolean todos, Set<UUID> uuids) {
        if (todos) return new ArrayList<>(server.getPlayerList().getPlayers());
        List<ServerPlayer> res = new ArrayList<>();
        for (UUID u : uuids) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) res.add(p);
        }
        return res;
    }

    /**
     * Muestra el aviso. Si su duración es "permanente" queda guardado (lo verá quien entre después
     * y sobrevive a reinicios); si no, se quita lo guardado.
     */
    public static int mostrar(MinecraftServer server, AvisoDatos datos, String id, Aviso aviso,
                              boolean todos, Set<UUID> uuids) {
        if (aviso.duracionSeg == 0) {
            datos.permanentes.put(id, new AvisoDatos.Permanente(todos, new HashSet<>(uuids)));
        } else {
            datos.permanentes.remove(id);
        }
        datos.setDirty();
        List<ServerPlayer> dest = destinatarios(server, todos, uuids);
        enviar(dest, id, aviso);
        return dest.size();
    }

    public static void enviar(Collection<ServerPlayer> jugadores, String id, Aviso a) {
        if (jugadores.isEmpty()) return;
        AvisoPaquete paquete = new AvisoPaquete(id, a.titulo, a.texto, a.pista, a.fuente,
                a.colorBarra, a.colorIcono, a.colorTitulo, a.colorTexto,
                a.icono.ordinal(), a.zona.ordinal(), a.escala, a.opacidad, a.margen,
                a.duracionSeg * 1000L);
        for (ServerPlayer j : jugadores) {
            ModNetwork.CANAL.send(PacketDistributor.PLAYER.with(() -> j), paquete);
        }
    }
}
