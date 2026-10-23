package com.example.avisos;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Nombres de colores en español, o código hexadecimal de 6 dígitos (ej: ff0000). */
public final class Colores {
    public static final Map<String, Integer> NOMBRES = new LinkedHashMap<>();

    static {
        NOMBRES.put("rojo", 0xFF5555);
        NOMBRES.put("rojooscuro", 0xAA0000);
        NOMBRES.put("naranja", 0xFFAA00);
        NOMBRES.put("amarillo", 0xFFFF55);
        NOMBRES.put("verde", 0x55FF55);
        NOMBRES.put("verdeoscuro", 0x00AA00);
        NOMBRES.put("cian", 0x55FFFF);
        NOMBRES.put("azul", 0x5555FF);
        NOMBRES.put("azuloscuro", 0x0000AA);
        NOMBRES.put("morado", 0xAA00AA);
        NOMBRES.put("rosa", 0xFF55FF);
        NOMBRES.put("blanco", 0xFFFFFF);
        NOMBRES.put("gris", 0xAAAAAA);
        NOMBRES.put("negro", 0x000000);
    }

    private Colores() {}

    /** Devuelve el color RGB (0xRRGGBB) o -1 si no es válido. */
    public static int parsear(String t) {
        String s = t.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("#")) s = s.substring(1);
        Integer c = NOMBRES.get(s);
        if (c != null) return c;
        if (s.matches("[0-9a-f]{6}")) return Integer.parseInt(s, 16);
        return -1;
    }

    public static String texto(int rgb) {
        for (Map.Entry<String, Integer> e : NOMBRES.entrySet()) {
            if (e.getValue() == rgb) return e.getKey();
        }
        return String.format("%06x", rgb & 0xFFFFFF);
    }
}
