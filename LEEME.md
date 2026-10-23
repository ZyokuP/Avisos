# Avisos en Pantalla (Forge 1.20.1)

Tarjeta de aviso en la pantalla (barra de color arriba, icono redondo, encabezado, texto y una línea gris
"[I] para ocultar aviso"). Por defecto aparece arriba a la izquierda y en verde. Todo es personalizable.
Comandos solo para operadores. Los jugadores ocultan/muestran los avisos con la tecla I
(se puede cambiar en Opciones > Controles > Avisos).

## El aviso verde "TP"
```
/aviso crear tp TP
/aviso poner tp
```

## Un aviso rojo
```
/aviso crear peligro Zona restringida
/aviso config peligro color rojo          (barra, icono, encabezado y texto, todo rojo)
/aviso config peligro titulo ALERTA:      (donde dice NOTIFICACION:)
/aviso config peligro icono alerta
/aviso poner peligro
```

## Todos los ajustes: /aviso config <nombre> ...
- `titulo <texto>`  `texto <texto>` (usa \n para saltos de línea)
- `color <nombre|ff0000>` cambia todo; `color barra|icono|titulo|texto <color>` solo una parte
  (nombres: rojo, rojooscuro, naranja, amarillo, verde, verdeoscuro, cian, azul, azuloscuro, morado, rosa, blanco, gris, negro)
- `icono anillo|punto|cuadrado|alerta|ninguno`
- `posicion arriba-izquierda|arriba-centro|arriba-derecha|izquierda|centro|derecha|abajo-izquierda|abajo-centro|abajo-derecha`
- `escala 50-300`  `opacidad 0-100` (fondo)  `margen <px>`
- `duracion 10s|5m|permanente`
- `pista <texto|ninguna>` (la línea gris; {tecla} se cambia por la tecla real)
- `fuente <id>` (fuente de un resource pack, ej. minecraft:uniform) | `fuente normal`

Otros: `/aviso poner <nombre> Steve` (solo para Steve), `/aviso quitar <nombre>`, `/aviso limpiar`,
`/aviso borrar <nombre>`, `/aviso lista`, `/aviso config <nombre>` (ver ajustes).
Un aviso "permanente" queda fijo para todos (también para quien entre después) y sobrevive a reinicios.

## Obtener el .jar sin instalar nada
1. Sube TODO el contenido de esta carpeta (incluida `.github`) a un repositorio de GitHub.
2. Pestaña Actions -> "Compilar mod" -> descarga **avisos-jar** (dentro está el .jar).
3. Ponlo en `mods/` del servidor y de cada jugador.
