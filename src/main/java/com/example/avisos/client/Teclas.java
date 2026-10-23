package com.example.avisos.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** La tecla para ocultar/mostrar los avisos (por defecto I; se cambia en Opciones > Controles). */
public final class Teclas {
    public static final KeyMapping OCULTAR =
            new KeyMapping("key.avisos.ocultar", InputConstants.KEY_I, "key.categories.avisos");

    private Teclas() {}

    public static String nombreTecla() {
        return OCULTAR.getTranslatedKeyMessage().getString();
    }
}
