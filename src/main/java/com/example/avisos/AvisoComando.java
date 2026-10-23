package com.example.avisos;

import com.example.avisos.network.ModNetwork;
import com.example.avisos.network.QuitarPaquete;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * /aviso crear <nombre> <texto...>        -> crea el aviso (verde, como la captura)
 * /aviso poner <nombre> [jugadores]       -> lo muestra (sin jugadores = todos)
 * /aviso quitar <nombre> [jugadores] | limpiar [jugadores] | borrar <nombre> | lista
 * /aviso config <nombre> titulo|texto|color|icono|posicion|escala|opacidad|margen|duracion|pista|fuente ...
 * Solo operadores.
 */
@Mod.EventBusSubscriber(modid = AvisoMod.MODID)
public class AvisoComando {

    private static final String[] SUGERENCIAS_COLOR = {
            "rojo", "naranja", "amarillo", "verde", "cian", "azul", "morado", "rosa", "blanco", "gris"};

    @SubscribeEvent
    public static void registrar(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("aviso")
                .requires(src -> src.hasPermission(2))
                .executes(c -> ayuda(c.getSource()));

        raiz.then(Commands.literal("crear")
                .then(Commands.argument("nombre", StringArgumentType.word())
                        .then(Commands.argument("texto", StringArgumentType.greedyString())
                                .executes(c -> crear(c.getSource(), StringArgumentType.getString(c, "nombre"),
                                        StringArgumentType.getString(c, "texto"))))));

        raiz.then(Commands.literal("poner")
                .then(nombreArg()
                        .executes(c -> poner(c.getSource(), nom(c), true, List.of()))
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(c -> poner(c.getSource(), nom(c), false,
                                        EntityArgument.getPlayers(c, "jugadores"))))));

        raiz.then(Commands.literal("quitar")
                .then(nombreArg()
                        .executes(c -> quitar(c.getSource(), nom(c), true, List.of()))
                        .then(Commands.argument("jugadores", EntityArgument.players())
                                .executes(c -> quitar(c.getSource(), nom(c), false,
                                        EntityArgument.getPlayers(c, "jugadores"))))));

        raiz.then(Commands.literal("limpiar")
                .executes(c -> limpiar(c.getSource(), true, List.of()))
                .then(Commands.argument("jugadores", EntityArgument.players())
                        .executes(c -> limpiar(c.getSource(), false, EntityArgument.getPlayers(c, "jugadores")))));

        raiz.then(Commands.literal("borrar").then(nombreArg().executes(c -> borrar(c.getSource(), nom(c)))));
        raiz.then(Commands.literal("lista").executes(c -> lista(c.getSource())));

        // ---------------- config
        RequiredArgumentBuilder<CommandSourceStack, String> cfg = nombreArg()
                .executes(c -> verConfig(c.getSource(), nom(c)));

        cfg.then(Commands.literal("titulo").then(Commands.argument("texto", StringArgumentType.greedyString())
                .executes(c -> {
                    String v = limpio(StringArgumentType.getString(c, "texto"));
                    if (v.length() > 100) return fallo(c.getSource(), "El título es demasiado largo (máx. 100).");
                    return ajustar(c.getSource(), nom(c), a -> a.titulo = v, "Título: " + v);
                })));

        cfg.then(Commands.literal("texto").then(Commands.argument("texto", StringArgumentType.greedyString())
                .executes(c -> {
                    String v = limpio(StringArgumentType.getString(c, "texto"));
                    if (v.length() > 400) return fallo(c.getSource(), "El texto es demasiado largo (máx. 400).");
                    return ajustar(c.getSource(), nom(c), a -> a.texto = v, "Texto: " + v);
                })));

        // color (todo) y color por partes
        LiteralArgumentBuilder<CommandSourceStack> color = Commands.literal("color");
        color.then(colorArg().executes(c -> {
            int rgb = Colores.parsear(StringArgumentType.getString(c, "color"));
            if (rgb < 0) return colorInvalido(c.getSource());
            return ajustar(c.getSource(), nom(c), a -> {
                a.colorBarra = rgb;
                a.colorIcono = rgb;
                a.colorTitulo = rgb;
                a.colorTexto = rgb;
            }, "Color de todo el aviso: " + Colores.texto(rgb));
        }));
        color.then(parte("barra", (a, rgb) -> a.colorBarra = rgb));
        color.then(parte("icono", (a, rgb) -> a.colorIcono = rgb));
        color.then(parte("titulo", (a, rgb) -> a.colorTitulo = rgb));
        color.then(parte("texto", (a, rgb) -> a.colorTexto = rgb));
        cfg.then(color);

        LiteralArgumentBuilder<CommandSourceStack> icono = Commands.literal("icono");
        for (Icono i : Icono.values()) {
            icono.then(Commands.literal(i.nombre).executes(c ->
                    ajustar(c.getSource(), nom(c), a -> a.icono = i, "Icono: " + i.nombre)));
        }
        cfg.then(icono);

        LiteralArgumentBuilder<CommandSourceStack> pos = Commands.literal("posicion");
        for (Posicion z : Posicion.values()) {
            pos.then(Commands.literal(z.nombre).executes(c ->
                    ajustar(c.getSource(), nom(c), a -> a.zona = z, "Posición: " + z.nombre)));
        }
        cfg.then(pos);

        cfg.then(Commands.literal("escala").then(Commands.argument("porcentaje", IntegerArgumentType.integer(50, 300))
                .executes(c -> {
                    int v = IntegerArgumentType.getInteger(c, "porcentaje");
                    return ajustar(c.getSource(), nom(c), a -> a.escala = v, "Escala: " + v + "%");
                })));

        cfg.then(Commands.literal("opacidad").then(Commands.argument("porcentaje", IntegerArgumentType.integer(0, 100))
                .executes(c -> {
                    int v = IntegerArgumentType.getInteger(c, "porcentaje");
                    return ajustar(c.getSource(), nom(c), a -> a.opacidad = v, "Opacidad del fondo: " + v + "%");
                })));

        cfg.then(Commands.literal("margen").then(Commands.argument("pixeles", IntegerArgumentType.integer(0, 100))
                .executes(c -> {
                    int v = IntegerArgumentType.getInteger(c, "pixeles");
                    return ajustar(c.getSource(), nom(c), a -> a.margen = v, "Margen: " + v + " px");
                })));

        cfg.then(Commands.literal("duracion")
                .then(Commands.literal("permanente").executes(c ->
                        ajustar(c.getSource(), nom(c), a -> a.duracionSeg = 0, "Duración: permanente")))
                .then(Commands.argument("tiempo", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                new String[]{"10s", "30s", "1m", "5m", "permanente"}, b))
                        .executes(c -> duracion(c.getSource(), nom(c), StringArgumentType.getString(c, "tiempo")))));

        cfg.then(Commands.literal("pista")
                .then(Commands.literal("ninguna").executes(c ->
                        ajustar(c.getSource(), nom(c), a -> a.pista = "", "Línea de ayuda quitada")))
                .then(Commands.argument("texto", StringArgumentType.greedyString())
                        .executes(c -> {
                            String v = limpio(StringArgumentType.getString(c, "texto"));
                            if (v.length() > 200) return fallo(c.getSource(), "Demasiado largo (máx. 200).");
                            return ajustar(c.getSource(), nom(c), a -> a.pista = v, "Línea de ayuda: " + v);
                        })));

        cfg.then(Commands.literal("fuente")
                .then(Commands.literal("normal").executes(c ->
                        ajustar(c.getSource(), nom(c), a -> a.fuente = "", "Fuente: la normal")))
                .then(Commands.argument("id", StringArgumentType.greedyString())
                        .executes(c -> {
                            String v = StringArgumentType.getString(c, "id").trim();
                            if (ResourceLocation.tryParse(v) == null) return fallo(c.getSource(), "Id de fuente no válido.");
                            return ajustar(c.getSource(), nom(c), a -> a.fuente = v, "Fuente: " + v);
                        })));

        raiz.then(Commands.literal("config").then(cfg));

        e.getDispatcher().register(raiz);
    }

    // ------------------------------------------------------------------ construcción

    private static RequiredArgumentBuilder<CommandSourceStack, String> nombreArg() {
        return Commands.argument("nombre", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                        AvisoDatos.get(ctx.getSource().getServer()).avisos.keySet(), b));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> colorArg() {
        return Commands.argument("color", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(SUGERENCIAS_COLOR, b));
    }

    private interface AplicaColor {
        void aplicar(Aviso a, int rgb);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> parte(String nombre, AplicaColor f) {
        return Commands.literal(nombre).then(colorArg().executes(c -> {
            int rgb = Colores.parsear(StringArgumentType.getString(c, "color"));
            if (rgb < 0) return colorInvalido(c.getSource());
            return ajustar(c.getSource(), nom(c), a -> f.aplicar(a, rgb),
                    "Color de " + nombre + ": " + Colores.texto(rgb));
        }));
    }

    private static String nom(CommandContext<CommandSourceStack> c) {
        return StringArgumentType.getString(c, "nombre");
    }

    /** Permite escribir \n para saltos de línea. */
    private static String limpio(String s) {
        return s.replace("\\n", "\n");
    }

    private static int fallo(CommandSourceStack src, String mensaje) {
        src.sendFailure(Component.literal(mensaje));
        return 0;
    }

    private static int colorInvalido(CommandSourceStack src) {
        return fallo(src, "Color no válido. Usa un nombre (rojo, azul, verde...) o un código de 6 dígitos como ff0000.");
    }

    // ------------------------------------------------------------------ comandos

    private static int ayuda(CommandSourceStack src) {
        String t = """
                === Avisos en pantalla ===
                /aviso crear <nombre> <texto>  -> crea un aviso (verde, como el de ejemplo)
                /aviso poner <nombre> [jugadores]  -> lo muestra (sin jugadores = todos)
                /aviso quitar <nombre> [jugadores]  |  /aviso limpiar [jugadores]
                /aviso borrar <nombre>  |  /aviso lista
                /aviso config <nombre>  -> ve sus ajustes
                /aviso config <nombre> titulo <texto>   (donde dice NOTIFICACION:)
                /aviso config <nombre> texto <texto>
                /aviso config <nombre> color <rojo|azul|ff0000...>  (cambia todo)
                /aviso config <nombre> color <barra|icono|titulo|texto> <color>
                /aviso config <nombre> icono <anillo|punto|cuadrado|alerta|ninguno>
                /aviso config <nombre> posicion <zona>  |  escala  |  opacidad  |  margen
                /aviso config <nombre> duracion <10s|5m|permanente>
                /aviso config <nombre> pista <texto|ninguna>   ({tecla} = la tecla)
                Los jugadores ocultan/muestran los avisos con una tecla (por defecto I; se cambia en Controles).""";
        src.sendSuccess(() -> Component.literal(t), false);
        return 1;
    }

    private static int crear(CommandSourceStack src, String nombre, String texto) {
        if (nombre.length() > 32) return fallo(src, "El nombre es demasiado largo (máx. 32).");
        String v = limpio(texto);
        if (v.length() > 400) return fallo(src, "El texto es demasiado largo (máx. 400).");

        MinecraftServer server = src.getServer();
        AvisoDatos datos = AvisoDatos.get(server);
        Aviso a = datos.avisos.get(nombre);
        boolean nuevo = a == null;
        if (nuevo) {
            a = new Aviso();
            datos.avisos.put(nombre, a);
        }
        a.texto = v;
        datos.setDirty();

        AvisoDatos.Permanente p = datos.permanentes.get(nombre);
        boolean enPantalla = p != null;
        if (enPantalla) Servicio.mostrar(server, datos, nombre, a, p.todos, p.jugadores);

        boolean esNuevo = nuevo;
        src.sendSuccess(() -> Component.literal((esNuevo ? "Aviso '" + nombre + "' creado. Muéstralo con /aviso poner " + nombre + "."
                : "Texto de '" + nombre + "' actualizado.")), true);
        return 1;
    }

    private static int poner(CommandSourceStack src, String nombre, boolean todos, Collection<ServerPlayer> jugadores) {
        MinecraftServer server = src.getServer();
        AvisoDatos datos = AvisoDatos.get(server);
        Aviso a = datos.avisos.get(nombre);
        if (a == null) return fallo(src, "No existe el aviso '" + nombre + "'. Créalo con /aviso crear " + nombre + " <texto>.");

        Set<UUID> uuids = new HashSet<>();
        if (!todos) for (ServerPlayer j : jugadores) uuids.add(j.getUUID());

        int n = Servicio.mostrar(server, datos, nombre, a, todos, uuids);
        String cuanto = a.duracionSeg == 0 ? "fijo en pantalla" : "durante " + TiempoUtil.formatear(a.duracionSeg);
        src.sendSuccess(() -> Component.literal("Aviso '" + nombre + "' mostrado a " + n + " jugador(es), " + cuanto
                + (a.duracionSeg == 0 && todos ? " (también a quien entre después)." : ".")), true);
        return n;
    }

    private static int quitar(CommandSourceStack src, String nombre, boolean todos, Collection<ServerPlayer> jugadores) {
        MinecraftServer server = src.getServer();
        AvisoDatos datos = AvisoDatos.get(server);

        Collection<ServerPlayer> objetivo = jugadores;
        if (todos) {
            datos.permanentes.remove(nombre);
            objetivo = server.getPlayerList().getPlayers();
        } else {
            AvisoDatos.Permanente p = datos.permanentes.get(nombre);
            if (p != null && !p.todos) {
                for (ServerPlayer j : jugadores) p.jugadores.remove(j.getUUID());
                if (p.jugadores.isEmpty()) datos.permanentes.remove(nombre);
            }
        }
        datos.setDirty();

        QuitarPaquete paquete = new QuitarPaquete(nombre);
        for (ServerPlayer j : objetivo) {
            ModNetwork.CANAL.send(PacketDistributor.PLAYER.with(() -> j), paquete);
        }
        int n = objetivo.size();
        src.sendSuccess(() -> Component.literal("Aviso '" + nombre + "' quitado para " + n + " jugador(es)."), true);
        return n;
    }

    private static int limpiar(CommandSourceStack src, boolean todos, Collection<ServerPlayer> jugadores) {
        MinecraftServer server = src.getServer();
        AvisoDatos datos = AvisoDatos.get(server);

        Collection<ServerPlayer> objetivo = jugadores;
        if (todos) {
            datos.permanentes.clear();
            objetivo = server.getPlayerList().getPlayers();
        } else {
            List<String> vacios = new ArrayList<>();
            for (Map.Entry<String, AvisoDatos.Permanente> e : datos.permanentes.entrySet()) {
                if (e.getValue().todos) continue;
                for (ServerPlayer j : jugadores) e.getValue().jugadores.remove(j.getUUID());
                if (e.getValue().jugadores.isEmpty()) vacios.add(e.getKey());
            }
            for (String k : vacios) datos.permanentes.remove(k);
        }
        datos.setDirty();

        QuitarPaquete paquete = new QuitarPaquete("");
        for (ServerPlayer j : objetivo) {
            ModNetwork.CANAL.send(PacketDistributor.PLAYER.with(() -> j), paquete);
        }
        int n = objetivo.size();
        src.sendSuccess(() -> Component.literal("Avisos quitados para " + n + " jugador(es)."), true);
        return n;
    }

    private static int borrar(CommandSourceStack src, String nombre) {
        MinecraftServer server = src.getServer();
        AvisoDatos datos = AvisoDatos.get(server);
        if (datos.avisos.remove(nombre) == null) return fallo(src, "No existe el aviso '" + nombre + "'.");
        datos.permanentes.remove(nombre);
        datos.setDirty();
        QuitarPaquete paquete = new QuitarPaquete(nombre);
        for (ServerPlayer j : server.getPlayerList().getPlayers()) {
            ModNetwork.CANAL.send(PacketDistributor.PLAYER.with(() -> j), paquete);
        }
        src.sendSuccess(() -> Component.literal("Aviso '" + nombre + "' borrado."), true);
        return 1;
    }

    private static int lista(CommandSourceStack src) {
        AvisoDatos datos = AvisoDatos.get(src.getServer());
        StringBuilder sb = new StringBuilder("Avisos (").append(datos.avisos.size()).append("):");
        for (Map.Entry<String, Aviso> e : datos.avisos.entrySet()) {
            Aviso a = e.getValue();
            sb.append("\n- ").append(e.getKey()).append(": \"").append(a.texto.replace("\n", " ")).append("\"  [")
                    .append(Colores.texto(a.colorBarra)).append(", ").append(a.zona.nombre).append(", ")
                    .append(a.duracionSeg == 0 ? "permanente" : TiempoUtil.formatear(a.duracionSeg)).append("]");
            if (datos.permanentes.containsKey(e.getKey())) sb.append("  << EN PANTALLA");
        }
        String texto = sb.toString();
        src.sendSuccess(() -> Component.literal(texto), false);
        return 1;
    }

    private static int verConfig(CommandSourceStack src, String nombre) {
        Aviso a = AvisoDatos.get(src.getServer()).avisos.get(nombre);
        if (a == null) return fallo(src, "No existe el aviso '" + nombre + "'.");
        String t = "Ajustes de '" + nombre + "':\n"
                + "titulo: " + a.titulo + "\n"
                + "texto: " + a.texto.replace("\n", "\\n") + "\n"
                + "colores: barra=" + Colores.texto(a.colorBarra) + " icono=" + Colores.texto(a.colorIcono)
                + " titulo=" + Colores.texto(a.colorTitulo) + " texto=" + Colores.texto(a.colorTexto) + "\n"
                + "icono: " + a.icono.nombre + "  posicion: " + a.zona.nombre + "\n"
                + "escala: " + a.escala + "%  opacidad: " + a.opacidad + "%  margen: " + a.margen + " px\n"
                + "duracion: " + (a.duracionSeg == 0 ? "permanente" : TiempoUtil.formatear(a.duracionSeg)) + "\n"
                + "pista: " + (a.pista.isEmpty() ? "(ninguna)" : a.pista)
                + (a.fuente.isEmpty() ? "" : "\nfuente: " + a.fuente);
        src.sendSuccess(() -> Component.literal(t), false);
        return 1;
    }

    private static int duracion(CommandSourceStack src, String nombre, String texto) {
        long seg = TiempoUtil.parsear(texto);
        if (seg < 1 || seg > 86400) {
            return fallo(src, "Duración no válida. Ejemplos: 10s, 5m, 1h, permanente (máx. 24h).");
        }
        return ajustar(src, nombre, a -> a.duracionSeg = (int) seg, "Duración: " + TiempoUtil.formatear(seg));
    }

    /** Cambia un ajuste y, si el aviso está fijo en pantalla, lo actualiza al instante. */
    private static int ajustar(CommandSourceStack src, String nombre, Consumer<Aviso> cambio, String mensaje) {
        MinecraftServer server = src.getServer();
        AvisoDatos datos = AvisoDatos.get(server);
        Aviso a = datos.avisos.get(nombre);
        if (a == null) return fallo(src, "No existe el aviso '" + nombre + "'. Créalo con /aviso crear " + nombre + " <texto>.");
        cambio.accept(a);
        datos.setDirty();

        AvisoDatos.Permanente p = datos.permanentes.get(nombre);
        boolean enPantalla = p != null;
        if (enPantalla) Servicio.mostrar(server, datos, nombre, a, p.todos, p.jugadores);

        src.sendSuccess(() -> Component.literal(mensaje + " (" + nombre + ")"
                + (enPantalla ? " - actualizado en pantalla." : ". Se verá la próxima vez que lo muestres.")), true);
        return 1;
    }
}
