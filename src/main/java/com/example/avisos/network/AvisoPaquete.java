package com.example.avisos.network;

import com.example.avisos.client.ClienteAvisos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: "muestra este aviso así". */
public class AvisoPaquete {
    private final String id;
    private final String titulo;
    private final String texto;
    private final String pista;
    private final String fuente;
    private final int colorBarra;
    private final int colorIcono;
    private final int colorTitulo;
    private final int colorTexto;
    private final int icono;
    private final int zona;
    private final int escala;
    private final int opacidad;
    private final int margen;
    private final long duracionMs;

    public AvisoPaquete(String id, String titulo, String texto, String pista, String fuente,
                        int colorBarra, int colorIcono, int colorTitulo, int colorTexto,
                        int icono, int zona, int escala, int opacidad, int margen, long duracionMs) {
        this.id = id;
        this.titulo = titulo;
        this.texto = texto;
        this.pista = pista;
        this.fuente = fuente;
        this.colorBarra = colorBarra;
        this.colorIcono = colorIcono;
        this.colorTitulo = colorTitulo;
        this.colorTexto = colorTexto;
        this.icono = icono;
        this.zona = zona;
        this.escala = escala;
        this.opacidad = opacidad;
        this.margen = margen;
        this.duracionMs = duracionMs;
    }

    public static void codificar(AvisoPaquete m, FriendlyByteBuf b) {
        b.writeUtf(m.id, 64);
        b.writeUtf(m.titulo, 128);
        b.writeUtf(m.texto, 512);
        b.writeUtf(m.pista, 256);
        b.writeUtf(m.fuente, 128);
        b.writeInt(m.colorBarra);
        b.writeInt(m.colorIcono);
        b.writeInt(m.colorTitulo);
        b.writeInt(m.colorTexto);
        b.writeVarInt(m.icono);
        b.writeVarInt(m.zona);
        b.writeVarInt(m.escala);
        b.writeVarInt(m.opacidad);
        b.writeVarInt(m.margen);
        b.writeLong(m.duracionMs);
    }

    public static AvisoPaquete decodificar(FriendlyByteBuf b) {
        String id = b.readUtf(64);
        String titulo = b.readUtf(128);
        String texto = b.readUtf(512);
        String pista = b.readUtf(256);
        String fuente = b.readUtf(128);
        int cBarra = b.readInt();
        int cIcono = b.readInt();
        int cTitulo = b.readInt();
        int cTexto = b.readInt();
        int icono = b.readVarInt();
        int zona = b.readVarInt();
        int escala = b.readVarInt();
        int opacidad = b.readVarInt();
        int margen = b.readVarInt();
        long dur = b.readLong();
        return new AvisoPaquete(id, titulo, texto, pista, fuente, cBarra, cIcono, cTitulo, cTexto,
                icono, zona, escala, opacidad, margen, dur);
    }

    public static void manejar(AvisoPaquete m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClienteAvisos.mostrar(new ClienteAvisos.Datos(m.id, m.titulo, m.texto, m.pista, m.fuente,
                        m.colorBarra, m.colorIcono, m.colorTitulo, m.colorTexto,
                        m.icono, m.zona, m.escala, m.opacidad, m.margen, m.duracionMs))));
        ctx.get().setPacketHandled(true);
    }
}
