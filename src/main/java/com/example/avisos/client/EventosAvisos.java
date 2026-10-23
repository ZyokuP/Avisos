package com.example.avisos.client;

import com.example.avisos.AvisoMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registros del cliente (tecla y HUD). El HUD se dibuja justo debajo del chat. */
public class EventosAvisos {

    @Mod.EventBusSubscriber(modid = AvisoMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class Registro {
        @SubscribeEvent
        public static void teclas(RegisterKeyMappingsEvent e) {
            e.register(Teclas.OCULTAR);
        }

        @SubscribeEvent
        public static void hud(RegisterGuiOverlaysEvent e) {
            e.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "avisos",
                    (gui, g, parcial, ancho, alto) -> ClienteAvisos.dibujar(g, ancho, alto));
        }
    }

    @Mod.EventBusSubscriber(modid = AvisoMod.MODID, value = Dist.CLIENT)
    public static class Juego {
        @SubscribeEvent
        public static void alTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            while (Teclas.OCULTAR.consumeClick()) {
                ClienteAvisos.alternarOculto();
            }
        }

        @SubscribeEvent
        public static void alSalir(ClientPlayerNetworkEvent.LoggingOut e) {
            ClienteAvisos.limpiarTodo();
        }
    }
}
