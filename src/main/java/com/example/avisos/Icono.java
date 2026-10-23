package com.example.avisos;

/** Formas del icono redondo (la "ruletita") que va junto al encabezado. */
public enum Icono {
    ANILLO("anillo"),
    PUNTO("punto"),
    CUADRADO("cuadrado"),
    ALERTA("alerta"),
    NINGUNO("ninguno");

    public final String nombre;

    Icono(String nombre) {
        this.nombre = nombre;
    }

    public static Icono deIndice(int i) {
        Icono[] v = values();
        return (i >= 0 && i < v.length) ? v[i] : ANILLO;
    }
}
