# Proyecto Integrador — Sistema de Solicitudes de Reembolso de Gastos Corporativos

Unidad 12 · Patrones de Diseño de Software (UDES). Spring Boot 3.2 · Java 17 · Maven.

## Diseño Propuesto

> Sección escrita **antes** de implementar los patrones (primer commit del repositorio). Para cada requisito se
> identifica qué varía, qué patrón lo resuelve y por qué ese y no otro (test de explicabilidad, Guía §1.2).

| Requisito | ¿Qué varía? (punto de variabilidad) | Patrón propuesto | Justificación en una oración |
|-----------|-------------------------------------|------------------|------------------------------|
| **RF1** Validación en varias etapas | El **conjunto y el orden de las verificaciones independientes** (hoy 3; Finanzas quiere agregar, p. ej., un límite anual acumulado). | **Chain of Responsibility** | Cada verificación decide por sí sola si rechaza o pasa a la siguiente sin conocer a las demás, así que agregar el límite anual es sumar un eslabón sin tocar los existentes — un `if` encadenado o un método único obligaría a editar código ya probado. |
| **RF2** Cálculo por categoría | La **fórmula del monto aprobado**, distinta por categoría (viáticos, representación, capacitación; próximamente "bienestar"). | **Strategy** (selección con mapa inyectado) | Es un algoritmo intercambiable según un dato (la categoría) y cada nueva categoría debe ser una clase nueva, no una modificación de las existentes (OCP), lo que un `switch` no permite. |
| **RF3** Notificación a varios interesados | **Quiénes reaccionan** a un cambio de estado y **cómo** (empleado: todo; contabilidad: solo aprobaciones; auditoría: todo; mañana, un panel de métricas). | **Observer** | Un único cambio de estado debe llegar *simultáneamente* a N receptores con reacciones distintas y N crece, por lo que el servicio solo debe conocer la interfaz `ObservadorCambioEstado` (Strategy elegiría *uno*; aquí se notifican *todos*). |

**Por qué los tres patrones son distintos entre sí:** RF1 es una *secuencia con corte anticipado* (Chain), RF2 es *elegir
uno entre varios algoritmos* (Strategy) y RF3 es *difundir a todos los suscriptores* (Observer). Ninguno puede
reemplazar a otro sin perder el problema real que resuelve.

## Arquitectura resultante

```
adaptadores.rest ──► aplicacion ──► procesamiento (patrones) ──► dominio
        │                │
        │                └──► infraestructura (JPA)
        └──────────────────────► dominio

dominio/           SolicitudReembolso, CategoriaGasto, EstadoSolicitud   (sin Spring ni JPA)
procesamiento/
  validacion/      ValidadorSolicitud (abstracta) · 3 validadores · CadenaValidadores      [Chain of Responsibility]
  calculo/         CalculadoraReembolso (interfaz) · Viaje / Representacion / Capacitacion  [Strategy]
  SelectorCalculadora   mapa categoria → calculadora, construido con List<CalculadoraReembolso>
  notificacion/    ObservadorCambioEstado (interfaz) · Empleado / Contabilidad / Auditoria  [Observer]
aplicacion/        SolicitudReembolsoService (coordina los 3 patrones) · excepciones de aplicacion
infraestructura/   RepositorioSolicitudesJpa (JpaRepository)
adaptadores/rest/  SolicitudReembolsoController · ManejadorErroresRest
```

Flujo de `POST /api/solicitudes`: el servicio carga las pendientes del empleado → `CadenaValidadores` (documentos → monto →
duplicados) → `SelectorCalculadora` elige la estrategia → se guarda `PENDIENTE`. Flujo de `PUT /api/solicitudes/{id}/estado`:
se actualiza el estado → el servicio notifica a **todos** los `ObservadorCambioEstado` registrados.

**Nota sobre el mapeo JPA:** para que el dominio cumpla "no importa ninguna clase de Spring ni de JPA" y aun así se
persista con `JpaRepository<SolicitudReembolso, Long>`, el mapeo se declara en `src/main/resources/META-INF/orm.xml`
en lugar de anotar la clase con `@Entity`.

## Decisiones de diseño

### Decisión de diseño 1 — Orden de la cadena: documentos → monto → duplicados
Se ubica `ValidadorDuplicados` **al final** y los dos más baratos al principio (*fail-fast*):
- `ValidadorDocumentosSoporte` solo lee un booleano (O(1)) y `ValidadorMontoMaximo` hace un lookup en un mapa (O(1));
  `ValidadorDuplicados` recorre la lista de pendientes del empleado (O(n)), el eslabón más caro.
- **Qué se gana:** las solicitudes que se rechazan por documentos o por monto (los errores más comunes y baratos de
  detectar) nunca pagan el recorrido de la lista. Con el orden inverso, toda solicitud pagaría ese costo aunque fuera a
  ser rechazada después.
- **Qué se pierde:** el empleado solo ve el **primer** motivo de rechazo; si su solicitud además es duplicada, lo
  descubrirá recién tras corregir documentos y monto. Es un trade-off consciente: un único mensaje claro por intento.
- **Matiz honesto:** la consulta a la base de datos que carga las pendientes la hace el servicio *antes* de la cadena,
  así que el costo que se ahorra es el recorrido en memoria, no la consulta. Hacerla perezosa sería optimizar sin una
  medición que lo pida (YAGNI).

### Decisión de diseño 2 — `Map` armado desde `List<CalculadoraReembolso>` en vez de `switch`
Se elige el mapa construido con la lista que Spring inyecta. Con un `switch` sobre `CategoriaGasto`, agregar
"bienestar" obliga a **editar** `SelectorCalculadora` (código ya escrito y probado). Con el mapa, la nueva categoría es una
clase nueva con `@Component` y `getCategoria()`; el selector no cambia una sola línea (OCP), que es exactamente lo que pide RF2.
- **Límite que se reconoce:** agregar la categoría sigue requiriendo una constante nueva en el enum `CategoriaGasto` y
  una entrada en el mapa de topes de `ValidadorMontoMaximo` (por eso ese validador rechaza con mensaje claro una
  categoría sin tope en vez de fallar con `NullPointerException`). Señal para reconsiderar: si los topes siguen
  creciendo, moverlos a la propia estrategia o a configuración.

### Decisión de diseño 3 — Capas simples para persistencia; el seam de notificación ya basta (ADR-001)
`SolicitudReembolsoService` sigue dependiendo directamente de `RepositorioSolicitudesJpa`, sin puerto intermedio, y
`procesamiento.notificacion` no cambia para el anuncio de Contabilidad. Con datos del proyecto: **persistencia** no tiene un
segundo motor en el horizonte, así que la Hexagonal sería una abstracción sin un segundo adaptador (no se cumple el criterio
de la Guía §6.2 "3 años o más *con cambios de infraestructura*" para este eje); **notificación** sí tiene un cambio
de infraestructura con fecha (API externa de Contabilidad), pero `ObservadorCambioEstado` ya es el punto de extensión: basta
con cambiar la implementación interna de `NotificadorContabilidad`. La respuesta es **distinta para cada eje**, y está
documentada con alternativas y consecuencias en [`docs/adr/ADR-001.md`](docs/adr/ADR-001.md).

### Decisión de diseño 4 — La Regla 1 distingue estereotipos de Spring de acceso a datos y capa web
`procesamiento` prohíbe `..infraestructura..`, `..adaptadores..`, `org.springframework.data..` y `jakarta.persistence..`, pero **no** todo
`org.springframework`. Razones:
- `@Component` es una anotación de **estereotipo**: solo marca el bean y no acopla la clase a ninguna capa. Las clases siguen
  siendo POJOs que se instancian con `new` en las pruebas unitarias (ver `ValidadoresTest` y `CalculadorasTest`, que no levantan Spring).
- Spring Data y JPA sí atan una clase al acceso a datos, y `..adaptadores..` a la capa web: **esos** son los límites que
  la arquitectura quiere proteger, y la regla apunta exactamente a ellos.
- Prohibir cualquier import de `org.springframework` obligaría a mover el cableado a clases `@Configuration` con `@Bean`
  (más código y más indirección) sin ganar desacoplamiento real: una regla demasiado amplia genera falsos positivos y acaba
  desactivada. La regla debe ser lo bastante precisa para que cuando falle, signifique algo.

## Resumen de ADR-001 (2026-10-30, Aceptado)
- **Contexto:** ¿migrar a Hexagonal completa o mantener capas simples tras integrar los tres patrones? Contabilidad
  anunció una integración por API externa para el próximo trimestre; no hay plan de cambiar de base de datos.
- **Alternativas:** (1) Hexagonal completa — descartada para persistencia (YAGNI); (2) capas simples + seam de Observer —
  **elegida**; (3) puerto de salida solo para pagos — diferida hasta tener el contrato de la API.
- **Decisión:** capas simples para persistencia; sin cambios en notificación porque `ObservadorCambioEstado` ya es el seam.
- **Consecuencias:** (+) sin interfaces especulativas, cambio conocido ya cubierto; (−) deuda consciente si cambia el motor
  de BD, riesgo de latencia/fallo de la API síncrona, posible lectura de "asimetría" por un revisor.
- Texto completo: [`docs/adr/ADR-001.md`](docs/adr/ADR-001.md).

## Qué se descartó y por qué
Criterio (Guía §4.2): ¿la variabilidad que justificaría la alternativa es **real y documentada hoy**, o es especulación?
Ninguno es un descarte definitivo: cada uno indica la señal que lo reabriría.

### 1. `ApplicationEventPublisher` + `@EventListener` en vez de `List<ObservadorCambioEstado>` *(ejemplo de referencia)*
- **Se consideró:** publicar un evento de dominio con el bus de eventos de Spring (Guía §2.3).
- **Se descartó:** con exactamente tres observadores fijos y conocidos en compilación, el bus no aporta desacoplamiento
  real (ya lo da la interfaz) y dificulta rastrear quién reacciona a qué (el costo de depuración que la Guía advierte).
- **Señal para reconsiderar:** observadores en otros módulos/servicios, o necesidad de procesamiento asíncrono o transaccional (`@TransactionalEventListener`).

### 2. Visitor o Abstract Factory para exportar reportes en varios formatos *(ejemplo de referencia)*
- **Se consideró:** anticipar PDF y Excel además de CSV con una jerarquía de exportadores.
- **Se descartó:** solo existe el requisito de CSV; sin una segunda solicitud real no se cumple la Rule of Three (Guía §4.2).
- **Señal para reconsiderar:** el segundo formato real solicitado; con el tercero, extraer la abstracción.

### 3. Jerarquía de excepciones por validador *(elemento propio)*
- **Se consideró:** una excepción por verificación (`DocumentosFaltantesException`, `MontoExcedidoException`,
  `SolicitudDuplicadaException`…) en lugar de una sola `SolicitudInvalidaException` con el motivo como texto.
- **Se descartó:** hoy todas las fallas se tratan **igual** (HTTP 400 con el motivo); ningún código distingue una de otra
  con un `catch` específico, así que 3 clases serían estructura sin comportamiento diferente (fallo del test de explicabilidad:
  no hay una oración que justifique su existencia). Además, cada validador nuevo (p. ej. el límite anual) obligaría a crear otra clase.
- **Señal para reconsiderar:** que un consumidor necesite reaccionar distinto por tipo de falla (por ejemplo, `REQUIERE_CORRECCION`
  automático para documentos faltantes pero rechazo duro para duplicados), o códigos de error estables para un cliente externo.

### 4. Abstract Factory / Builder para construir `SolicitudReembolso` *(elemento propio)*
- **Se consideró:** una fábrica por categoría que construyera la solicitud con sus campos propios (p. ej. `diasViaje` solo aplica a viajes).
- **Se descartó:** la construcción es un POJO con setters, sin invariantes complejas ni lógica condicional de creación; el
  único campo específico de categoría es `diasViaje`. Una fábrica resolvería un problema que todavía no existe (Rule of Three: hay 0 casos de construcción compleja).
- **Señal para reconsiderar:** que dos o más categorías exijan campos propios obligatorios o reglas de creación distintas.

### 5. DTOs y mapper en la capa REST *(elemento propio, deuda consciente)*
- **Se consideró:** `SolicitudRequestDTO`/`SolicitudResponseDTO` en lugar de recibir y devolver el modelo de dominio.
- **Se descartó por ahora:** la API es de uso interno de Finanzas y el dominio no tiene campos sensibles que ocultar; un DTO
  1:1 duplicaría la clase sin aportar. (En el laboratorio de la Unidad 11 sí se justificaba: había un campo `notasInternas` que filtrar.)
- **Señal para reconsiderar:** que la API se publique a terceros, que el dominio gane campos internos, o que la forma del JSON deba evolucionar independiente del modelo.

## Cómo compilar y ejecutar
Requisitos: Java 17+ y Maven 3.9+.
```bash
mvn clean package          # compila y corre todas las pruebas
mvn test -Dtest=ReglasArquitectura   # solo las 4 reglas ArchUnit (Parte 2)
mvn spring-boot:run        # arranca en http://localhost:8080
./pruebas-curl.sh          # con la app corriendo: ejecuta los casos de prueba
```

| Método | Ruta | Cuerpo | Respuestas |
|--------|------|--------|-----------|
| POST | `/api/solicitudes` | JSON con `empleadoId, categoria, montoSolicitado, fechaGasto, diasViaje, documentosSoporte` | 200 con `montoAprobado` y estado `PENDIENTE` · 400 con el motivo |
| PUT | `/api/solicitudes/{id}/estado` | JSON string: `"APROBADA"`, `"RECHAZADA"` o `"REQUIERE_CORRECCION"` (con comillas) | 200 · 404 |

## Pruebas
- **Unitarias:** `ValidadoresTest` (cada validador y la cadena) y `CalculadorasTest` (cada calculadora y el selector).
- **Integración:** `SolicitudReembolsoIT` (`@SpringBootTest`): flujo completo para las 3 categorías, rechazo por documentos y
  duplicado, y notificación a los tres observadores.
- **Arquitectura:** `ReglasArquitectura` (ArchUnit, Parte 2).

## Evidencia del pipeline (GitHub Actions)
El workflow `.github/workflows/arquitectura.yml` ejecuta las 4 reglas ArchUnit y luego `mvn verify` en cada push y pull request a `main`.

**Violación intencional (Regla 1):** se agregó a `ValidadorDuplicados` un campo `private RepositorioSolicitudesJpa repo;`
(`procesamiento` pasa a depender de `infraestructura`), se hizo push y el job `arquitectura` **falló** con
`procesamientoSinPersistenciaNiWeb`. Luego se revirtió con `git revert HEAD` y el pipeline volvió a verde.

