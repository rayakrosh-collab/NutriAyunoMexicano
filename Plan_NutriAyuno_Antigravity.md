# NutriAyuno MX — Plan de construcción y contexto para Antigravity

> **Documento maestro del proyecto.** Esto es lo que le vas a dar a Antigravity para que entienda
> *qué* construimos, *cómo* está estructurado y *en qué orden* avanzar. Está escrito para que un
> agente lo use como contexto y para que tú, con poca experiencia en Android, lo entiendas.
>
> **Nombre provisional:** *NutriAyuno MX* (nutrición + ayuno + México; cámbialo por el que más te guste).
>
> **Esta es tu segunda app**, así que varios conceptos (Room, Compose, freemium, publicar en Play) ya te
> sonarán del proyecto anterior. Vas a ir más rápido.

---

## 0. Cómo usar este documento

1. Abre Antigravity y crea un proyecto/carpeta nuevo.
2. Pega como contexto inicial el **Prompt de arranque** de la sección 9 (es el más importante).
3. Si puedes, pega también este documento completo como archivo de contexto del proyecto (un `CONTEXTO.md` en la raíz).
4. Avanza **una fase a la vez** (sección 8). Cada fase termina en un *checkpoint*: hasta que eso funcione, no pases a la siguiente.
5. Al terminar cada fase, guarda con git ("haz commit de lo que llevamos").

> Regla de oro: si no entiendes algo, dile a Antigravity *"explícamelo como si nunca hubiera programado Android"*.

---

## 1. Resumen del proyecto

**Qué es:** una app Android que combina un **temporizador de ayuno intermitente** con un **contador de proteína diaria**, apoyado en una **base de datos de comida mexicana y latina**.

**Para quién:** personas en México/LatAm que hacen ayuno intermitente y/o cuidan su proteína (empezando por ti).

**El gancho que la diferencia:** las apps de EE.UU. (MyFitnessPal, Zero, etc.) hacen mal la comida local — sus bases están llenas de productos gringos y fallan con tacos, frijoles, tinga, barbacoa, queso panela, nopales… Nuestra ventaja es una **base de alimentos pensada para México**, simple y centrada en lo que la gente come aquí.

**Enfoque deliberado:** no intentamos competir con un contador universal de calorías. Nos enfocamos en dos cosas que hacemos muy bien: **ayuno** + **proteína**, con comida local. Simple y claro.

**Idioma de la interfaz:** español (México). **Unidades:** sistema métrico (gramos, kg).

**Modelo de negocio:** *freemium* (gratis con anuncios + desbloqueo "Pro").

---

## 2. Alcance de la versión 1 (MVP)

### ✅ Dentro del alcance
- Temporizador de ayuno con protocolos (16:8, 18:6, 20:4, personalizado), inicio/fin y notificación al completar.
- Base de datos de alimentos (seed curado de comida mexicana) con búsqueda.
- Registro de comida y **total de proteína del día** contra una meta que el usuario define.
- Tablero con: ayuno actual, proteína de hoy, racha de ayunos y gráficas semanales.
- Recordatorios configurables (inicio/fin de ayuno).
- Identidad visual y textos en español.
- Monetización: anuncios (AdMob) + desbloqueo Pro (Play Billing).

### 🚫 Fuera del alcance de la v1 (para NO sobre-construir)
- Escaneo de código de barras (queda como fase opcional / v2).
- Conteo completo de calorías y todos los macros (la v1 se centra en **proteína**).
- Respaldo / sincronización en la nube.
- Cuentas de usuario / inicio de sesión.
- Planes de dieta, rutinas o recomendaciones médicas (ver sección 6).
- Versión para iOS.

> **Importante:** dile a Antigravity que respete este alcance. Mantenerlo chico es lo que hace que termines.

---

## 3. El reto central: la base de datos de comida mexicana

Esto es **tu diferenciador y también tu mayor esfuerzo**. Léelo con calma porque define el éxito de la app.

### Estrategia recomendada
- **Empieza con un *seed* curado y chico:** ~150–250 alimentos mexicanos comunes. NO intentes meter el universo de alimentos. Calidad sobre cantidad.
- **Céntrate en la proteína por porción** (es lo central de la app). Calorías y otros macros son opcionales.
- **Empaqueta el seed como un archivo** (JSON o CSV) dentro de los *assets* de la app. Room lo importa a la base de datos en el **primer arranque**.

### Qué guardar por alimento
- Nombre (ej. "Frijoles negros cocidos")
- Categoría (ej. leguminosas, carnes, lácteos, platillos, tortillas…)
- Porción base (descripción + gramos aprox.) — ej. "1 taza (~130 g)", "1 taco (~90 g)"
- **Proteína (g) por porción**
- (Opcional) calorías, carbohidratos, grasas
- Bandera de origen (mexicano/latino) y, a futuro, código de barras

### Cómo armar el seed (plan concreto)
1. Haz la lista de lo más consumido en México: tortilla de maíz/harina, frijoles, huevo, pollo, res, cerdo, atún/pescado, queso panela/Oaxaca, leche, yogurt, nopales, aguacate, avena, arroz… más platillos comunes (tacos, tinga, barbacoa, chilaquiles, pozole, guisados).
2. Asigna la proteína por porción usando como **referencia** tablas de composición de alimentos mexicanas (p. ej. el SMAE — Sistema Mexicano de Alimentos Equivalentes, o tablas del INSP). 
3. Antigravity te puede ayudar a **generar un primer JSON** con valores razonables, que luego **tú revisas y ajustas** (no lo publiques sin revisar).

> ⚠️ **Sobre derechos:** los valores nutricionales como dato (ej. "100 g de frijol tienen X g de proteína") son hechos y no se pueden "poseer". Pero una tabla o base concreta sí puede tener derechos sobre su compilación. Por eso la jugada correcta es **construir tu propia compilación** a partir de varias referencias, no copiar una tabla ajena tal cual.

### Expansión a futuro (v2)
- Integrar **Open Food Facts** (base de datos abierta y gratuita, con códigos de barras y cobertura de productos en México) para escanear códigos y ampliar el catálogo automáticamente.
- *Verifica su API y su licencia (ODbL) en el momento de integrarla.*

---

## 4. Stack tecnológico

| Pieza | Tecnología | Por qué |
|---|---|---|
| Lenguaje | **Kotlin** | El estándar moderno de Android. |
| Interfaz (UI) | **Jetpack Compose** + **Material 3** | UI declarativa; lo que Antigravity/AI Studio generan mejor. |
| Base de datos local | **Room** (sobre SQLite) | Aquí juegas en casa: es SQL con ayudantes. |
| Arquitectura | **MVVM** (ViewModel + Repositorio) | Separa la lógica de la pantalla. |
| Navegación | **Navigation Compose** | Para moverte entre pantallas. |
| Temporizador/avisos | **WorkManager** + Notificaciones | Para el ayuno y recordatorios aunque la app esté cerrada. |
| Gráficas | **Vico** (charts para Compose) | Moderna y compatible con Compose. |
| Anuncios | **Google AdMob** | Monetización por anuncios. |
| Compras | **Google Play Billing** | Para el desbloqueo "Pro". |
| (v2) Código de barras | **ML Kit Barcode Scanning** | Escanear productos para la base de alimentos. |

**Configuración base:**
- `minSdk = 26` (Android 8.0).
- `targetSdk = la versión más reciente que exija Google Play` (deja que Antigravity ponga el número actual).
- Solo modo *portrait* (vertical) en la v1.

---

## 5. Modelo de datos

Estas son las "tablas" (en Room, *entidades*). Antigravity las creará en la Fase 1.

### Entidad: `Alimento` (la base de datos de comida)
- `id` — clave primaria
- `nombre`
- `categoria`
- `porcionDescripcion` — ej. "1 taza", "1 taco"
- `porcionGramos` *(nullable)*
- `proteinaG` — gramos de proteína por porción
- `caloriasKcal`, `carbohidratosG`, `grasasG` *(opcionales)*
- `origen` — mexicano / latino / genérico
- `codigoBarras` *(nullable, para v2)*

### Entidad: `RegistroComida` (lo que comes)
- `id` — clave primaria
- `alimentoId` — relación con `Alimento`
- `fecha`
- `cantidadPorciones` — cuántas porciones registraste
- `proteinaCalculadaG` — proteína × cantidad
- `momento` *(opcional: desayuno / comida / cena / snack)*

### Entidad: `SesionAyuno`
- `id` — clave primaria
- `inicio` — fecha/hora de inicio
- `fin` — fecha/hora de fin *(nullable mientras el ayuno está activo)*
- `horasObjetivo` — ej. 16
- `completada` — booleano

### Entidad: `PerfilAjustes` (una sola fila)
- `pesoKg` *(opcional)*
- `metaProteinaDiaria` — gramos; **la define el usuario**
- `protocoloAyunoPreferido` — ej. "16:8"
- `unidades` — métrico por defecto

### Cálculos (explícaselos a Antigravity)
- **Proteína del día:** suma de `proteinaCalculadaG` de los registros de esa fecha.
- **Progreso:** `proteína del día ÷ metaProteinaDiaria`.
- **Tiempo de ayuno en vivo:** `ahora − inicio` (mientras `fin` sea null).
- **Ayuno completado:** cuando el tiempo transcurrido ≥ `horasObjetivo`.
- **Racha:** días consecutivos con al menos un ayuno completado.

---

## 6. Diseño responsable (salud) — léelo, importa de verdad

Una app de ayuno y proteína toca el bienestar de la gente. Hacer esto bien no es solo ético: también evita que **Google Play te rechace** la app y construye confianza. Tres reglas:

1. **Sin promesas médicas ni de resultados.** Nada de "baja 10 kg" ni consejos clínicos. La app es una herramienta de registro, no un médico ni un nutriólogo. Incluye un aviso visible: *"Esta app es informativa; consulta a un profesional de salud antes de hacer cambios en tu alimentación o ayuno."*
2. **El usuario manda y sin presión.** La meta de proteína la fija el usuario (puedes ofrecer una sugerencia informativa basada en rangos generales, pero opcional y con disclaimer). Nada de mecánicas de culpa o castigo por no cumplir; los recordatorios son amables y desactivables. Los protocolos de ayuno por defecto son moderados (16:8), **no** promuevas ayunos extremos o prolongados como opción destacada.
3. **No dirigida a menores.** Configura la clasificación y la ficha de Play en consecuencia.

---

## 7. Principios de diseño / UX

- **Simple antes que bonito-complicado.** La pantalla principal debe responder de un vistazo: "¿estoy en ayuno?" y "¿cuánta proteína llevo hoy?".
- Registrar comida o iniciar ayuno debe estar a **un toque** desde el inicio.
- **Español natural de México** en todos los textos.
- **Material 3** con un color principal + modo claro/oscuro.
- Tono motivador y positivo, nunca de regaño.

---

## 8. Plan de construcción por fases

Cada fase tiene **objetivo**, **qué se construye**, **qué aprendes** y un **checkpoint** (cómo saber que funciona).

### Fase 0 — Preparar el entorno y el proyecto base
- **Objetivo:** proyecto creado y abriendo una pantalla en el emulador o tu teléfono.
- **Qué se construye:** proyecto Kotlin + Compose, dependencias base, pantalla "Hola NutriAyuno".
- **Qué aprendes:** estructura del proyecto, Gradle, correr en el emulador.
- **Checkpoint:** la app abre y muestra el texto de bienvenida. ✅

### Fase 1 — Capa de datos (Room) + importar el seed de alimentos
- **Objetivo:** crear la base de datos local y cargar los alimentos.
- **Qué se construye:** las 4 entidades (sección 5), sus DAOs, la `RoomDatabase`, repositorios, y un **importador** que lee el archivo seed (JSON/CSV) de los *assets* y llena la tabla `Alimento` en el primer arranque.
- **Qué aprendes:** entidades, DAO, relaciones, leer datos, e importar datos iniciales desde un asset.
- **Checkpoint:** la base tiene los alimentos del seed (verlos en Logcat o lista temporal) y persisten. ✅
- **Nota:** empieza con un seed pequeño de prueba (10–20 alimentos); lo amplías en la sección 3 más adelante.

### Fase 2 — Temporizador de ayuno (el corazón, lo más demoable)
- **Objetivo:** iniciar/terminar ayunos y ver el tiempo correr.
- **Qué se construye:** elegir protocolo (16:8, 18:6, 20:4, personalizado), botón iniciar/terminar, cuenta de progreso en vivo, guardar la sesión, historial de ayunos, y **notificación al completar** la meta.
- **Qué aprendes:** manejo de tiempo/estado en vivo, notificaciones, WorkManager.
- **Checkpoint:** inicias un ayuno (prueba con objetivo de 1 min), ves el tiempo correr, te llega la notificación al completar y queda en el historial. ✅

### Fase 3 — Buscar alimentos en la base
- **Objetivo:** encontrar comida y ver su proteína.
- **Qué se construye:** pantalla de búsqueda, lista de resultados, pantalla de detalle del alimento (proteína por porción).
- **Qué aprendes:** búsquedas/consultas filtradas en Room, listas en Compose.
- **Checkpoint:** buscas "frijoles" y ves su proteína por porción. ✅

### Fase 4 — Registrar comida y total de proteína del día
- **Objetivo:** sumar la proteína del día contra tu meta.
- **Qué se construye:** agregar un alimento al día (elegir cantidad), ver el **total de proteína de hoy vs la meta**, lista de lo comido hoy, editar/borrar; y una pantalla de **ajustes** para fijar la meta de proteína.
- **Qué aprendes:** relacionar datos (registro ↔ alimento), cálculos, formularios.
- **Checkpoint:** registras 2–3 alimentos y ves tu total de proteína del día y el progreso hacia tu meta. ✅

### Fase 5 — Tablero de resumen + gráficas
- **Objetivo:** ver todo de un vistazo.
- **Qué se construye:** pantalla principal con el ayuno actual, proteína de hoy vs meta, **racha de ayunos**, y gráficas semanales (proteína diaria y ayunos completados) con Vico.
- **Qué aprendes:** juntar datos de varias tablas y graficarlos.
- **Checkpoint:** el tablero muestra tus datos reales de la semana. ✅

### Fase 6 — Recordatorios y pulido de protocolos
- **Objetivo:** avisos útiles y amables.
- **Qué se construye:** recordatorio de inicio/fin de la ventana de ayuno, recordatorio opcional de proteína (sin presión), permisos de notificación (Android 13+).
- **Qué aprendes:** programar notificaciones y manejar permisos.
- **Checkpoint:** recibes recordatorios configurables. ✅

### Fase 7 — Identidad visual, tema y localización
- **Objetivo:** que se vea pro y 100% en español.
- **Qué se construye:** tema Material 3 (color, claro/oscuro), ícono, pantalla de inicio (splash), textos en `strings.xml`, unidades en gramos/kg.
- **Checkpoint:** la app se ve consistente, con tu identidad y todo en español. ✅

### Fase 8 — Monetización (AdMob + Pro)
- **Objetivo:** activar el modelo freemium.
- **Qué se construye:**
  - **Gratis:** temporizador completo, registro con base de alimentos básica, historial corto (ej. últimos 7 días), banner discreto.
  - **Pro (desbloqueo único, ~$99–149 MXN):** sin anuncios, protocolos personalizados, historial completo, base de alimentos completa, exportar datos (y a futuro, escaneo de código de barras).
- **Qué aprendes:** AdMob, Play Billing y cómo "bloquear" funciones detrás del Pro.
- **Checkpoint:** ves un banner de **prueba** y el flujo de compra de **prueba** desbloquea Pro. ✅
- **Nota:** usa siempre IDs de prueba de AdMob y licencias de prueba de Play hasta publicar.

### Fase 9 — Preparar y publicar en Google Play
- **Objetivo:** subir la app a la pista de prueba.
- **Qué se construye:** App Bundle (.aab) firmado, **política de privacidad** (necesaria por AdMob y por registrar datos de hábitos), ficha de tienda (capturas, descripción, ícono), sección de seguridad de datos, clasificación de contenido, subir a *internal testing*.
- **Qué aprendes:** firma de apps, requisitos de Play, ASO básico.
- **Checkpoint:** tu app está en la pista de prueba y la puedes instalar desde Play. ✅

> ⚠️ **Cosas que tienes que hacer tú directamente:** crear tu **cuenta de Google Play Console** (pago único ~$25 USD; si ya la creaste para tu primera app, ¡ya la tienes!), capturar datos de pago, aceptar términos y publicar. Y ojo extra para esta categoría: **las apps de salud/dieta deben cumplir las políticas de Play** — evita afirmaciones médicas y promesas de resultados (ver sección 6).

### Fase opcional (v2) — Escaneo de código de barras
- **Objetivo:** agregar alimentos escaneando productos.
- **Qué se construye:** cámara + ML Kit Barcode + consulta a Open Food Facts para autollenar el alimento por su código de barras.
- **Nota:** función futura/opcional. NO la metas en la v1.

---

## 9. Prompts listos para Antigravity

### 🚀 Prompt de arranque (pégalo como primer mensaje)

```
Vas a ser mi mentor y guía paso a paso para construir una app Android.
Tengo poca experiencia en Android: NO asumas que domino Kotlin, Jetpack
Compose ni las herramientas. (Sí tengo experiencia en bases de datos y
backend, así que con SQL y lógica voy bien. Es mi segunda app.)

Vamos a construir "NutriAyuno MX": un temporizador de ayuno intermitente
+ contador de proteína diaria, con una base de datos de comida mexicana.
Enfoque deliberado: ayuno + proteína con comida local; NO un contador
universal de calorías.

Stack: Kotlin + Jetpack Compose + Material 3, Room para base de datos local,
arquitectura MVVM, Navigation Compose, WorkManager para notificaciones,
gráficas con Vico, AdMob y Google Play Billing. minSdk 26, solo vertical.

Importante (diseño responsable): la app es informativa, sin promesas médicas
ni de resultados, el usuario fija sus propias metas, recordatorios amables y
desactivables, protocolos de ayuno moderados por defecto, no dirigida a menores.

Trabajaremos por FASES, en orden, sin saltarnos ninguna. (Te paso el plan de
fases y el modelo de datos a continuación / por separado.)

Cómo quiero que me guíes:
1. Avanza UNA fase a la vez. No empieces la siguiente hasta que yo confirme.
2. Explícame cada paso como si nunca hubiera programado Android.
3. Dime EXACTAMENTE qué archivo crear, dónde, y qué código poner (completo).
4. Cuando uses un concepto nuevo (ViewModel, DAO, Composable, WorkManager),
   explícame en 1–2 frases qué es.
5. Al final de cada fase, dime cómo PROBAR que funciona (el "checkpoint").
6. Respeta el alcance de la v1; no agregues funciones extra que no pedí.

Empecemos por la Fase 0: preparar el entorno y el proyecto base.
Guíame paso a paso.
```

### 🔁 Para avanzar de fase

```
La Fase [N] funciona, ya probé el checkpoint y todo bien.
Antes de seguir, ayúdame a hacer commit de lo que llevamos.
Luego empecemos la Fase [N+1] con las mismas reglas: paso a paso,
explicándome todo y diciéndome qué archivos y código necesito.
```

### 🧾 Para generar el seed de alimentos (úsalo en la sección 3 / Fase 1)

```
Ayúdame a crear un archivo JSON con ~150 alimentos mexicanos comunes para
el seed de la app. Cada alimento debe tener: nombre, categoría, porción
(descripción + gramos aprox.), y proteína en gramos por porción.
Usa valores razonables basados en composición de alimentos típica.
Dame el JSON y explícame cómo colocarlo en los assets para que Room lo
importe en el primer arranque. Yo revisaré y ajustaré los valores.
```

### 🆘 Si te atoras

```
No entiendo esto: [pega el error o la duda].
Explícamelo como a un principiante total en Android, dime por qué pasa
y exactamente cómo arreglarlo, paso a paso.
```

---

## 10. Glosario rápido

- **Gradle:** arma (compila) tu app y maneja las librerías.
- **SDK / minSdk / targetSdk:** el kit de Android; versión mínima soportada / versión para la que optimizas.
- **Emulador:** un teléfono Android virtual en tu compu para probar.
- **Jetpack Compose / Composable:** la forma moderna de hacer pantallas; un Composable dibuja un pedazo de interfaz.
- **Room / Entity / DAO:** base de datos local; Entity = tabla, DAO = las consultas.
- **Assets / seed data:** archivos que vienen empacados con la app (como tu JSON de alimentos) que se cargan al arrancar.
- **ViewModel:** guarda el estado y la lógica de una pantalla.
- **State / Flow:** la fuente de la verdad de los datos; cuando cambia, la pantalla se actualiza sola.
- **Navigation:** cómo te mueves entre pantallas.
- **WorkManager:** ejecuta tareas y avisos aunque la app esté cerrada (clave para el ayuno).
- **ML Kit:** librería de Google para cosas como leer códigos de barras (v2).
- **Open Food Facts:** base de datos abierta de productos con códigos de barras (v2).
- **APK / AAB:** el instalable; Play pide AAB, el APK sirve para pruebas directas.
- **AdMob / Play Billing:** anuncios / compras dentro de la app.
- **ASO:** optimizar la ficha de Play para que te encuentren.
- **Logcat:** la consola donde ves mensajes y errores mientras corre la app.

---

## 11. Checklist de publicación (Fase 9)

- [ ] Cuenta de Google Play Console (si ya la tienes de tu primera app, listo) — *lo haces tú*
- [ ] Ícono (512×512) y gráfico de cabecera (1024×500)
- [ ] Capturas de pantalla (mínimo 2)
- [ ] Descripción corta y larga (en español, con palabras clave: "ayuno intermitente", "proteína", "comida mexicana")
- [ ] **Política de privacidad** publicada (URL)
- [ ] **Aviso de que la app es informativa** y no sustituye consejo médico (en la app y en la ficha)
- [ ] Sección de seguridad de datos (Data Safety) llenada
- [ ] Clasificación de contenido (no dirigida a menores)
- [ ] App Bundle (.aab) firmado y generado
- [ ] App subida a *Internal testing* y probada en tu teléfono
- [ ] (Después) promocionarla en tu canal **NullPointer** 🎥

---

*Hecho para Raúl — tu segunda app. Una fase a la vez. 🍳⏱️*
