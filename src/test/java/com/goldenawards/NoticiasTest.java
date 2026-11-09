package com.goldenawards;

import com.goldenawards.controller.NoticiaControladorRest;
import com.goldenawards.controller.PeriodistaControladorRest;
import com.goldenawards.model.Noticia;
import com.goldenawards.model.Usuario;
import com.goldenawards.repository.EdicionRepository;
import com.goldenawards.repository.NoticiaRepository;
import com.goldenawards.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * T016 - Pruebas de aceptacion del Periodista (Sprint 1).
 *  AC-PER-002.1: un borrador NO es visible en la pagina publica; al publicarlo si lo es.
 *  REQ-PER-003: la noticia guarda autor, fecha y edicion; solo el autor puede editarla o publicarla.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NoticiasTest {

    @Mock private UsuarioRepository usuarios;
    @Mock private NoticiaRepository noticias;
    @Mock private EdicionRepository ediciones;

    private final List<Noticia> almacen = new ArrayList<>();
    private PeriodistaControladorRest periodistaRest;
    private NoticiaControladorRest publicoRest;
    private Usuario periodista;

    @BeforeEach
    void preparar() {
        periodista = new Usuario();
        periodista.setId("P1");
        periodista.setNombre("Pablo Periodista");
        periodista.setUsername("periodista");
        periodista.setRol("PERIODISTA");

        // Repositorio en memoria: guarda en la lista y la consulta de publicadas se calcula al momento
        when(noticias.save(any(Noticia.class))).thenAnswer(inv -> {
            Noticia n = inv.getArgument(0);
            if (n.getId() == null) {
                n.setId("N" + (almacen.size() + 1));
            }
            if (!almacen.contains(n)) {
                almacen.add(n);
            }
            return n;
        });
        when(noticias.findByEstado("PUBLICADA")).thenAnswer(inv ->
                almacen.stream().filter(n -> "PUBLICADA".equals(n.getEstado())).toList());
        when(noticias.findById(anyString())).thenAnswer(inv ->
                almacen.stream().filter(n -> n.getId().equals(inv.getArgument(0))).findFirst());
        when(ediciones.existsById("E1")).thenReturn(true);
        when(ediciones.existsById("NOEXISTE")).thenReturn(false);

        periodistaRest = new PeriodistaControladorRest(usuarios, noticias, ediciones);
        publicoRest = new NoticiaControladorRest(noticias, ediciones);
    }

    @Test
    void unBorradorNoEsVisibleYAlPublicarloSi() {
        Noticia borrador = periodistaRest.crearBorrador(periodista, "Titulo", "Resumen", "Contenido", "E1");

        assertEquals("BORRADOR", borrador.getEstado());
        assertNull(publicoRest.visible(borrador.getId()), "un borrador no se ve en la pagina publica");
        assertTrue(publicoRest.publicadas(null).isEmpty());

        periodistaRest.publicar(borrador);

        assertEquals("PUBLICADA", borrador.getEstado());
        assertNotNull(borrador.getFechaPublicacion());
        assertNotNull(publicoRest.visible(borrador.getId()), "una noticia publicada si se ve");
        assertEquals(1, publicoRest.publicadas(null).size());
    }

    @Test
    void laNoticiaGuardaAutorFechaYEdicion() {
        Noticia n = periodistaRest.crearBorrador(periodista, "Titulo", null, "Contenido", "E1");

        assertEquals("P1", n.getAutorId());
        assertEquals("Pablo Periodista", n.getAutorNombre());
        assertEquals("E1", n.getEdicionId());
        assertNotNull(n.getFechaCreacion());
    }

    @Test
    void soloElAutorPuedeEditarOPublicar() {
        Noticia n = periodistaRest.crearBorrador(periodista, "Titulo", null, "Contenido", null);
        Usuario otro = new Usuario();
        otro.setId("P2");

        assertTrue(periodistaRest.esAutor(n, periodista));
        assertFalse(periodistaRest.esAutor(n, otro));
    }

    @Test
    void sePuedeFiltrarLasPublicadasPorEdicion() {
        Noticia deE1 = periodistaRest.crearBorrador(periodista, "Una", null, "x", "E1");
        Noticia sinEdicion = periodistaRest.crearBorrador(periodista, "Dos", null, "y", null);
        periodistaRest.publicar(deE1);
        periodistaRest.publicar(sinEdicion);

        assertEquals(2, publicoRest.publicadas(null).size());
        assertEquals(1, publicoRest.publicadas("E1").size());
    }

    @Test
    void validaTituloContenidoYEdicion() {
        assertNotNull(periodistaRest.validarCampos("", null, "texto", null));
        assertNotNull(periodistaRest.validarCampos("titulo", null, "  ", null));
        assertNotNull(periodistaRest.validarCampos("titulo", null, "texto", "NOEXISTE"));
        assertNotNull(periodistaRest.validarCampos("x".repeat(PeriodistaControladorRest.MAX_TITULO + 1), null, "texto", null));
        assertNull(periodistaRest.validarCampos("titulo", "resumen", "texto", "E1"));
    }
}
