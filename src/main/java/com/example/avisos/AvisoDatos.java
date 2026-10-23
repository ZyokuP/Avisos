package com.example.avisos;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Guarda en el mundo los avisos y cuáles están en pantalla de forma fija (sobrevive a reinicios). */
public class AvisoDatos extends SavedData {
    private static final String NOMBRE = "avisos_datos";

    /** Un aviso fijo: lo ve todo el que entre (todos) o solo ciertos jugadores. */
    public static final class Permanente {
        public final boolean todos;
        public final Set<UUID> jugadores;

        public Permanente(boolean todos, Set<UUID> jugadores) {
            this.todos = todos;
            this.jugadores = jugadores;
        }
    }

    public final Map<String, Aviso> avisos = new LinkedHashMap<>();
    public final Map<String, Permanente> permanentes = new LinkedHashMap<>();

    public static AvisoDatos get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(AvisoDatos::cargar, AvisoDatos::new, NOMBRE);
    }

    public static AvisoDatos cargar(CompoundTag tag) {
        AvisoDatos d = new AvisoDatos();

        ListTag lista = tag.getList("avisos", Tag.TAG_COMPOUND);
        for (int i = 0; i < lista.size(); i++) {
            CompoundTag t = lista.getCompound(i);
            Aviso a = new Aviso();
            a.titulo = t.getString("titulo");
            a.texto = t.getString("texto");
            a.pista = t.getString("pista");
            a.fuente = t.getString("fuente");
            a.colorBarra = t.getInt("cBarra");
            a.colorIcono = t.getInt("cIcono");
            a.colorTitulo = t.getInt("cTitulo");
            a.colorTexto = t.getInt("cTexto");
            a.icono = Icono.deIndice(t.getInt("icono"));
            a.zona = Posicion.deIndice(t.getInt("zona"));
            a.escala = clamp(t.getInt("escala"), 50, 300);
            a.opacidad = clamp(t.getInt("opacidad"), 0, 100);
            a.margen = clamp(t.getInt("margen"), 0, 100);
            a.duracionSeg = Math.max(0, t.getInt("dur"));
            d.avisos.put(t.getString("id"), a);
        }

        ListTag ps = tag.getList("permanentes", Tag.TAG_COMPOUND);
        for (int i = 0; i < ps.size(); i++) {
            CompoundTag t = ps.getCompound(i);
            Set<UUID> js = new HashSet<>();
            ListTag jl = t.getList("jugadores", Tag.TAG_STRING);
            for (int j = 0; j < jl.size(); j++) {
                try {
                    js.add(UUID.fromString(jl.getString(j)));
                } catch (IllegalArgumentException ignored) {
                }
            }
            d.permanentes.put(t.getString("id"), new Permanente(t.getBoolean("todos"), js));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag lista = new ListTag();
        for (Map.Entry<String, Aviso> e : avisos.entrySet()) {
            Aviso a = e.getValue();
            CompoundTag t = new CompoundTag();
            t.putString("id", e.getKey());
            t.putString("titulo", a.titulo);
            t.putString("texto", a.texto);
            t.putString("pista", a.pista);
            t.putString("fuente", a.fuente);
            t.putInt("cBarra", a.colorBarra);
            t.putInt("cIcono", a.colorIcono);
            t.putInt("cTitulo", a.colorTitulo);
            t.putInt("cTexto", a.colorTexto);
            t.putInt("icono", a.icono.ordinal());
            t.putInt("zona", a.zona.ordinal());
            t.putInt("escala", a.escala);
            t.putInt("opacidad", a.opacidad);
            t.putInt("margen", a.margen);
            t.putInt("dur", a.duracionSeg);
            lista.add(t);
        }
        tag.put("avisos", lista);

        ListTag ps = new ListTag();
        for (Map.Entry<String, Permanente> e : permanentes.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("id", e.getKey());
            t.putBoolean("todos", e.getValue().todos);
            ListTag jl = new ListTag();
            for (UUID u : e.getValue().jugadores) jl.add(StringTag.valueOf(u.toString()));
            t.put("jugadores", jl);
            ps.add(t);
        }
        tag.put("permanentes", ps);
        return tag;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
