package com.example.avisos.client;

import com.example.avisos.Icono;
import com.example.avisos.Posicion;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lado cliente: guarda los avisos activos y los dibuja en el HUD (tarjeta con barra de color arriba,
 * icono redondo, encabezado, texto y la línea gris de ayuda). Solo toca sus propios avisos.
 */
public class ClienteAvisos {
    private static final int FADE_MS = 300;
    private static final int PAD = 5;
    private static final int BARRA = 2;
    private static final int ICONO = 7;
    private static final int MAX_CUERPO = 170;

    public record Datos(String id, String titulo, String texto, String pista, String fuente,
                        int colorBarra, int colorIcono, int colorTitulo, int colorTexto,
                        int icono, int zona, int escala, int opacidad, int margen, long duracionMs) {}

    private static final class Activo {
        final Datos d;
        final long inicioNs = System.nanoTime();

        Activo(Datos d) {
            this.d = d;
        }
    }

    private static final Map<String, Activo> ACTIVOS = new LinkedHashMap<>();
    private static boolean oculto = false;

    // '#' = color del icono, 'o' = color del icono oscurecido, '.' = vacío
    private static final String[][] FORMAS = {
            // ANILLO: círculo con centro oscuro
            {"..###..", ".#####.", "###o###", "##ooo##", "###o###", ".#####.", "..###.."},
            // PUNTO: círculo lleno
            {"..###..", ".#####.", "#######", "#######", "#######", ".#####.", "..###.."},
            // CUADRADO
            {"#######", "#######", "#######", "#######", "#######", "#######", "#######"},
            // ALERTA: círculo con "!"
            {"..###..", ".##o##.", "###o###", "###o###", "#######", ".##o##.", "..###.."},
    };

    // ------------------------------------------------------------------ estado

    public static void mostrar(Datos d) {
        ACTIVOS.put(d.id(), new Activo(d));
        oculto = false; // un aviso nuevo siempre se ve
    }

    public static void quitar(String id) {
        if (id.isEmpty()) ACTIVOS.clear();
        else ACTIVOS.remove(id);
        if (ACTIVOS.isEmpty()) oculto = false;
    }

    public static void limpiarTodo() {
        ACTIVOS.clear();
        oculto = false;
    }

    /** Tecla "ocultar aviso": oculta o vuelve a mostrar los avisos de este jugador. */
    public static void alternarOculto() {
        if (!ACTIVOS.isEmpty()) oculto = !oculto;
    }

    // ------------------------------------------------------------------ dibujo

    public static void dibujar(GuiGraphics g, int sw, int sh) {
        if (ACTIVOS.isEmpty() || oculto) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return; // respeta F1

        Font font = mc.font;
        String tecla = Teclas.nombreTecla();
        long ahora = System.nanoTime();
        int[][] pila = new int[3][3]; // [vertical][horizontal]: cuánto se ha ocupado ya en esa zona

        Iterator<Activo> it = ACTIVOS.values().iterator();
        while (it.hasNext()) {
            Activo a = it.next();
            Datos d = a.d;

            long ms = (ahora - a.inicioNs) / 1_000_000L;
            float alfa = 1f;
            if (d.duracionMs() > 0) {
                if (ms >= d.duracionMs()) {
                    it.remove();
                    continue;
                }
                alfa = Math.min(1f, (d.duracionMs() - ms) / (float) FADE_MS);
            }
            alfa *= Math.min(1f, ms / (float) FADE_MS);
            if (alfa < 0.04f) continue; // (con alfa casi 0 el texto se dibujaría opaco)

            // ---- contenido
            ResourceLocation fuente = d.fuente().isEmpty() ? null : ResourceLocation.tryParse(d.fuente());
            MutableComponent titulo = Component.literal(d.titulo()).withStyle(ChatFormatting.BOLD);
            MutableComponent cuerpo = Component.literal(d.texto());
            if (fuente != null) {
                titulo = titulo.withStyle(s -> s.withFont(fuente));
                cuerpo = cuerpo.withStyle(s -> s.withFont(fuente));
            }
            List<FormattedCharSequence> lineas = d.texto().isEmpty() ? List.of() : font.split(cuerpo, MAX_CUERPO);
            String pista = d.pista().isEmpty() ? "" : d.pista().replace("{tecla}", tecla);
            boolean conIcono = d.icono() < Icono.NINGUNO.ordinal();

            // ---- medidas (en unidades sin escalar)
            int ancho = (conIcono ? ICONO + 4 : 0) + font.width(titulo);
            for (FormattedCharSequence l : lineas) ancho = Math.max(ancho, font.width(l));
            if (!pista.isEmpty()) ancho = Math.max(ancho, font.width(pista));
            ancho += 2 * PAD;

            int y = BARRA + PAD;
            int yTitulo = y;
            y += 9;
            int yCuerpo = y + 4;
            if (!lineas.isEmpty()) y = yCuerpo + 10 * lineas.size() - 1;
            int yPista = y + 4;
            if (!pista.isEmpty()) y = yPista + 9;
            int alto = y + PAD;

            // ---- posición en pantalla
            float escala = d.escala() / 100f;
            int anchoReal = Math.round(ancho * escala);
            int altoReal = Math.round(alto * escala);
            Posicion z = Posicion.deIndice(d.zona());
            int ocupado = pila[z.vertical][z.horizontal];

            int px = switch (z.horizontal) {
                case 0 -> d.margen();
                case 2 -> sw - anchoReal - d.margen();
                default -> (sw - anchoReal) / 2;
            };
            int py = switch (z.vertical) {
                case 0 -> d.margen() + ocupado;
                case 2 -> sh - altoReal - d.margen() - ocupado;
                default -> (sh - altoReal) / 2 + ocupado;
            };
            pila[z.vertical][z.horizontal] += altoReal + 4;

            // ---- dibujo
            g.pose().pushPose();
            g.pose().translate(px, py, 0);
            g.pose().scale(escala, escala, 1f);

            int fondo = ((int) (d.opacidad() / 100f * alfa * 255f) & 255) << 24;
            g.fill(0, 0, ancho, alto, fondo);
            g.fill(0, 0, ancho, BARRA, argb(d.colorBarra(), alfa));

            int xTitulo = PAD;
            if (conIcono) {
                dibujarIcono(g, FORMAS[d.icono()], PAD, yTitulo + 1, d.colorIcono(), alfa);
                xTitulo = PAD + ICONO + 4;
            }
            g.drawString(font, titulo, xTitulo, yTitulo, argb(d.colorTitulo(), alfa), true);

            int cy = yCuerpo;
            for (FormattedCharSequence l : lineas) {
                g.drawString(font, l, PAD, cy, argb(d.colorTexto(), alfa), true);
                cy += 10;
            }
            if (!pista.isEmpty()) {
                g.drawString(font, pista, PAD, yPista, argb(0xAAAAAA, alfa), true);
            }
            g.pose().popPose();
        }
    }

    private static void dibujarIcono(GuiGraphics g, String[] forma, int x, int y, int rgb, float alfa) {
        int claro = argb(rgb, alfa);
        int oscuro = argb(oscurecer(rgb), alfa);
        for (int r = 0; r < forma.length; r++) {
            for (int c = 0; c < forma[r].length(); c++) {
                char ch = forma[r].charAt(c);
                if (ch == '.') continue;
                g.fill(x + c, y + r, x + c + 1, y + r + 1, ch == 'o' ? oscuro : claro);
            }
        }
    }

    private static int argb(int rgb, float alfa) {
        return (((int) (alfa * 255f) & 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int oscurecer(int rgb) {
        int r = (int) (((rgb >> 16) & 255) * 0.4);
        int g = (int) (((rgb >> 8) & 255) * 0.4);
        int b = (int) ((rgb & 255) * 0.4);
        return (r << 16) | (g << 8) | b;
    }
}
