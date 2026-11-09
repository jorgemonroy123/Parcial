package com.goldenawards.config;

import com.goldenawards.model.*;
import com.goldenawards.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

// Carga datos de prueba la primera vez que la base de datos esta vacia
@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarios;
    private final JugadorRepository jugadores;
    private final EdicionRepository ediciones;
    private final CategoriaRepository categorias;
    private final NominacionRepository nominaciones;
    private final VotoRepository votos;
    private final ParametroRepository parametros;
    private final PuntajeRepository puntajes;
    private final RegistroAuditoriaRepository auditoria;
    private final NoticiaRepository noticias;
    private final PasswordEncoder encoder;

    public DataInitializer(UsuarioRepository usuarios, JugadorRepository jugadores,
                           EdicionRepository ediciones, CategoriaRepository categorias,
                           NominacionRepository nominaciones, VotoRepository votos,
                           ParametroRepository parametros, PuntajeRepository puntajes,
                           RegistroAuditoriaRepository auditoria, NoticiaRepository noticias,
                           PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.jugadores = jugadores;
        this.ediciones = ediciones;
        this.categorias = categorias;
        this.nominaciones = nominaciones;
        this.votos = votos;
        this.parametros = parametros;
        this.puntajes = puntajes;
        this.auditoria = auditoria;
        this.noticias = noticias;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (usuarios.count() == 0) {
            sembrarDatosBase();
        }
        // Se ejecutan siempre y solo agregan lo que falta (sirve tambien para bases que ya tenian datos)
        asegurarUsuariosDePrueba();
        migrarAuditoria();
        sembrarNoticias();
    }

    private void sembrarDatosBase() {

        // ---- Parametros globales ----
        parametros.save(parametro("nombre_plataforma", "Golden Awards", "Nombre que se muestra en la plataforma"));
        parametros.save(parametro("lema", "La noche donde el talento se corona", "Frase de la portada"));

        // ---- Catalogo de jugadores (ficticios) ----
        Jugador mateo = jugadores.save(jugador("Mateo Rivera", "Atletico Meridiano", "Delantero", "Colombia"));
        Jugador lucas = jugadores.save(jugador("Lucas Ferrante", "Real Costanera", "Mediocampista", "Argentina"));
        Jugador hugo = jugadores.save(jugador("Hugo Valverde", "FC Aurora", "Delantero", "Espana"));
        Jugador samuel = jugadores.save(jugador("Samuel Okoro", "Union del Norte", "Defensa", "Nigeria"));
        Jugador diego = jugadores.save(jugador("Diego Salazar", "Club Cumbres", "Portero", "Chile"));
        Jugador leandro = jugadores.save(jugador("Leandro Brito", "Estrella Roja Sur", "Portero", "Brasil"));
        Jugador tomas = jugadores.save(jugador("Tomas Aranda", "Deportivo Faro", "Mediocampista", "Uruguay"));
        Jugador emil = jugadores.save(jugador("Emil Lindqvist", "Nordic United", "Delantero", "Suecia"));
        Jugador yuki = jugadores.save(jugador("Yuki Tanabe", "Sakura FC", "Mediocampista", "Japon"));

        // ---- Usuarios de prueba (uno por rol + dos votantes extra para el conteo) ----
        usuarios.save(usuario("Administrador General", "admin", "admin123", "ADMIN", null));
        Usuario organizador = usuarios.save(usuario("Olivia Organizadora", "organizador", "organizador123", "ORGANIZADOR", null));
        Usuario votante = usuarios.save(usuario("Valeria Periodista", "votante", "votante123", "VOTANTE", null));
        Usuario votante2 = usuarios.save(usuario("Victor Capitan", "votante2", "votante123", "VOTANTE", null));
        Usuario votante3 = usuarios.save(usuario("Valentina Entrenadora", "votante3", "votante123", "VOTANTE", null));
        usuarios.save(usuario("Mateo Rivera", "nominado", "nominado123", "NOMINADO", mateo.getId()));
        usuarios.save(usuario("Paula Publico", "publico", "publico123", "PUBLICO", null));

        // ---- Edicion 2025: cerrada con resultados publicados ----
        Edicion e25 = new Edicion();
        e25.setNombre("Golden Awards 2025");
        e25.setAnio(2025);
        e25.setEstado("CERRADA");
        e25.setResultadosPublicados(true);
        e25 = ediciones.save(e25);

        Categoria b25 = categorias.save(categoria(e25, "Balon de Oro", "Al mejor jugador del ano"));
        Categoria p25 = categorias.save(categoria(e25, "Mejor Portero", "Al guardameta mas destacado"));
        Categoria j25 = categorias.save(categoria(e25, "Mejor Jugador Joven", "Al talento menor de 21 anos"));

        Nominacion n1 = nominaciones.save(nominacion(e25, b25, mateo, votante, "APROBADA"));
        Nominacion n2 = nominaciones.save(nominacion(e25, b25, lucas, votante, "APROBADA"));
        nominaciones.save(nominacion(e25, b25, hugo, votante, "APROBADA"));
        Nominacion n4 = nominaciones.save(nominacion(e25, p25, diego, votante, "APROBADA"));
        Nominacion n5 = nominaciones.save(nominacion(e25, p25, leandro, votante, "APROBADA"));
        Nominacion n6 = nominaciones.save(nominacion(e25, j25, tomas, votante, "APROBADA"));
        Nominacion n7 = nominaciones.save(nominacion(e25, j25, emil, votante, "APROBADA"));
        nominaciones.save(nominacion(e25, j25, yuki, votante, "APROBADA"));

        votos.save(voto(e25, b25, n1, votante));
        votos.save(voto(e25, b25, n1, votante2));
        votos.save(voto(e25, b25, n2, votante3));
        votos.save(voto(e25, p25, n4, votante));
        votos.save(voto(e25, p25, n5, votante2));
        votos.save(voto(e25, p25, n4, votante3));
        votos.save(voto(e25, j25, n6, votante));
        votos.save(voto(e25, j25, n7, votante2));
        votos.save(voto(e25, j25, n6, votante3));

        // ---- Edicion 2026: en periodo de votacion ----
        Edicion e26 = new Edicion();
        e26.setNombre("Golden Awards 2026");
        e26.setAnio(2026);
        e26.setEstado("VOTACION");
        e26.setResultadosPublicados(false);
        e26 = ediciones.save(e26);

        Categoria b26 = categorias.save(categoria(e26, "Balon de Oro", "Al mejor jugador del ano"));
        Categoria p26 = categorias.save(categoria(e26, "Mejor Portero", "Al guardameta mas destacado"));
        Categoria j26 = categorias.save(categoria(e26, "Mejor Jugador Joven", "Al talento menor de 21 anos"));

        Nominacion m1 = nominaciones.save(nominacion(e26, b26, mateo, organizador, "APROBADA"));
        Nominacion m2 = nominaciones.save(nominacion(e26, b26, lucas, organizador, "APROBADA"));
        nominaciones.save(nominacion(e26, b26, hugo, organizador, "APROBADA"));
        nominaciones.save(nominacion(e26, b26, samuel, votante2, "PENDIENTE"));
        nominaciones.save(nominacion(e26, p26, diego, organizador, "APROBADA"));
        nominaciones.save(nominacion(e26, p26, leandro, organizador, "APROBADA"));
        nominaciones.save(nominacion(e26, j26, tomas, organizador, "APROBADA"));
        nominaciones.save(nominacion(e26, j26, emil, organizador, "APROBADA"));
        nominaciones.save(nominacion(e26, j26, yuki, votante3, "PENDIENTE"));

        votos.save(voto(e26, b26, m2, votante2));
        votos.save(voto(e26, b26, m1, votante3));
    }

    // T006: un usuario de prueba por cada rol que no estaba en la semilla original
    private void asegurarUsuariosDePrueba() {
        asegurarUsuario("Juana Jurado", "jurado", "jurado123", "JURADO");
        asegurarUsuario("Andres Auditor", "auditor", "auditor123", "AUDITOR");
        asegurarUsuario("Marta Moderadora", "moderador", "moderador123", "MODERADOR");
        asegurarUsuario("Pablo Periodista", "periodista", "periodista123", "PERIODISTA");
        asegurarUsuario("Rosa Representante", "representante", "representante123", "REPRESENTANTE");
        asegurarUsuario("Sergio Soporte", "soporte", "soporte123", "SOPORTE");
    }

    private void asegurarUsuario(String nombre, String username, String password, String rol) {
        if (usuarios.findByUsername(username).isEmpty()) {
            usuarios.save(usuario(nombre, username, password, rol, null));
        }
    }

    // Los votos y calificaciones que ya existian (semilla) tambien aparecen en el registro del Auditor.
    // Nunca se copia el nominado elegido ni la nota (C-02).
    private void migrarAuditoria() {
        if (auditoria.count() > 0) {
            return;
        }
        Map<String, Usuario> porId = new HashMap<>();
        for (Usuario u : usuarios.findAll()) {
            porId.put(u.getId(), u);
        }
        for (Voto v : votos.findAll()) {
            Usuario u = porId.get(v.getVotanteId());
            if (u != null) {
                auditoria.save(registro(Auditoria.VOTO, u, v.getEdicionId(), v.getCategoriaId(), v.getFecha()));
            }
        }
        for (Puntaje p : puntajes.findAll()) {
            Usuario u = porId.get(p.getJuradoId());
            if (u != null) {
                auditoria.save(registro(Auditoria.CALIFICACION, u, p.getEdicionId(), p.getCategoriaId(), p.getFecha()));
            }
        }
    }

    private RegistroAuditoria registro(String tipo, Usuario u, String edicionId, String categoriaId, LocalDateTime fecha) {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setTipo(tipo);
        r.setUsuarioId(u.getId());
        r.setUsername(u.getUsername());
        r.setRol(u.getRol());
        r.setEdicionId(edicionId);
        r.setCategoriaId(categoriaId);
        r.setCanal("SEMILLA");
        r.setFecha(fecha != null ? fecha : LocalDateTime.now());
        return r;
    }

    // Noticias de demostracion: dos publicadas y un borrador del periodista de prueba
    private void sembrarNoticias() {
        if (noticias.count() > 0) {
            return;
        }
        Usuario periodista = usuarios.findByUsername("periodista").orElse(null);
        if (periodista == null) {
            return;
        }
        Edicion reciente = ediciones.findAll().stream().max(Comparator.comparingInt(Edicion::getAnio)).orElse(null);
        String edicionId = reciente == null ? null : reciente.getId();
        noticias.save(noticia(periodista, "Abre la votación del Golden Awards",
                "Los votantes ya pueden elegir a sus favoritos en cada categoría.",
                "El periodo de votación está abierto. Cada votante dispone de un único voto por categoría y podrá "
                        + "consultar su historial desde su panel.\n\nEl jurado también evalúa a los nominados y su "
                        + "calificación se sumará al ranking oficial.",
                edicionId, "PUBLICADA"));
        noticias.save(noticia(periodista, "Así se cuidan los votos: auditoría sin revelar el voto",
                "Un nuevo rol revisa la actividad de la votación sin saber por quién votó cada persona.",
                "El Auditor puede ver quién votó y cuándo, detectar intentos de voto duplicado y exportar un reporte. "
                        + "En ningún momento el registro guarda el nominado elegido: el voto sigue siendo secreto.",
                edicionId, "PUBLICADA"));
        noticias.save(noticia(periodista, "Borrador: crónica de la gala",
                "Aún sin publicar.",
                "Este texto es un borrador y solo lo ve su autor hasta que se publique.",
                edicionId, "BORRADOR"));
    }

    private Noticia noticia(Usuario autor, String titulo, String resumen, String contenido, String edicionId, String estado) {
        Noticia n = new Noticia();
        n.setTitulo(titulo);
        n.setResumen(resumen);
        n.setContenido(contenido);
        n.setEdicionId(edicionId);
        n.setAutorId(autor.getId());
        n.setAutorNombre(autor.getNombre());
        n.setEstado(estado);
        n.setFechaCreacion(LocalDateTime.now());
        if ("PUBLICADA".equals(estado)) {
            n.setFechaPublicacion(LocalDateTime.now());
        }
        return n;
    }

    private Parametro parametro(String clave, String valor, String descripcion) {
        Parametro p = new Parametro();
        p.setClave(clave);
        p.setValor(valor);
        p.setDescripcion(descripcion);
        return p;
    }

    private Jugador jugador(String nombre, String equipo, String posicion, String pais) {
        Jugador j = new Jugador();
        j.setNombre(nombre);
        j.setEquipo(equipo);
        j.setPosicion(posicion);
        j.setPais(pais);
        j.setBiografia(nombre + " es un futbolista de " + pais + " que juega como " + posicion + " en " + equipo + ".");
        int h = Math.abs(nombre.hashCode());
        j.setGoles(10 + (h % 40));
        j.setAsistencias(5 + (h % 25));
        j.setPartidos(20 + (h % 50));
        j.setTrofeos(h % 8);
        return j;
    }

    private Usuario usuario(String nombre, String username, String password, String rol, String jugadorId) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setRol(rol);
        u.setJugadorId(jugadorId);
        u.setActivo(true);
        return u;
    }

    private Categoria categoria(Edicion e, String nombre, String descripcion) {
        Categoria c = new Categoria();
        c.setEdicionId(e.getId());
        c.setNombre(nombre);
        c.setDescripcion(descripcion);
        return c;
    }

    private Nominacion nominacion(Edicion e, Categoria c, Jugador j, Usuario propuestoPor, String estado) {
        Nominacion n = new Nominacion();
        n.setEdicionId(e.getId());
        n.setCategoriaId(c.getId());
        n.setJugadorId(j.getId());
        n.setPropuestoPorId(propuestoPor.getId());
        n.setEstado(estado);
        n.setFecha(LocalDateTime.now());
        return n;
    }

    private Voto voto(Edicion e, Categoria c, Nominacion n, Usuario votante) {
        Voto v = new Voto();
        v.setEdicionId(e.getId());
        v.setCategoriaId(c.getId());
        v.setNominacionId(n.getId());
        v.setVotanteId(votante.getId());
        v.setFecha(LocalDateTime.now());
        return v;
    }
}
