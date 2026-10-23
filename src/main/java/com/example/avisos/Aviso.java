package com.example.avisos;

/** Definición de un aviso. Los valores por defecto reproducen el aviso verde de la captura. */
public class Aviso {
    public String titulo = "NOTIFICACION:";
    public String texto = "";
    /** Línea gris de abajo. {tecla} se reemplaza por la tecla real. Vacío = sin línea. */
    public String pista = "[{tecla}] para ocultar aviso";
    /** Fuente (recurso) opcional para titulo y texto, ej. minecraft:uniform. Vacío = la normal. */
    public String fuente = "";

    public int colorBarra = 0x55FF55;
    public int colorIcono = 0x55FF55;
    public int colorTitulo = 0x55FF55;
    public int colorTexto = 0x55FF55;

    public Icono icono = Icono.ANILLO;
    public Posicion zona = Posicion.ARRIBA_IZQUIERDA;
    /** Escala en % (50-300). */
    public int escala = 100;
    /** Opacidad del fondo en % (0-100). */
    public int opacidad = 70;
    /** Separación del borde de la pantalla. */
    public int margen = 4;
    /** Segundos en pantalla; 0 = permanente (hasta que se quite o el jugador lo oculte). */
    public int duracionSeg = 0;
}
