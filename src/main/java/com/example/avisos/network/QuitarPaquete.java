package com.example.avisos.network;

import com.example.avisos.client.ClienteAvisos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: quitar un aviso (id vacío = quitar todos). */
public class QuitarPaquete {
    private final String id;

    public QuitarPaquete(String id) {
        this.id = id;
    }

    public static void codificar(QuitarPaquete m, FriendlyByteBuf b) {
        b.writeUtf(m.id, 64);
    }

    public static QuitarPaquete decodificar(FriendlyByteBuf b) {
        return new QuitarPaquete(b.readUtf(64));
    }

    public static void manejar(QuitarPaquete m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClienteAvisos.quitar(m.id)));
        ctx.get().setPacketHandled(true);
    }
}
