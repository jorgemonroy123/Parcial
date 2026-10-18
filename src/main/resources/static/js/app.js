/* Golden Awards - comportamientos de interfaz */
document.addEventListener('DOMContentLoaded', function () {

    // ---- Tema claro / oscuro (se recuerda en el navegador) ----
    var btnTema = document.getElementById('btnTema');
    function pintarIcono() {
        if (!btnTema) return;
        var oscuro = document.documentElement.getAttribute('data-bs-theme') === 'dark';
        btnTema.innerHTML = oscuro ? '<i class="bi bi-brightness-high-fill"></i>' : '<i class="bi bi-moon-stars-fill"></i>';
    }
    pintarIcono();
    if (btnTema) {
        btnTema.addEventListener('click', function () {
            var nuevo = document.documentElement.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-bs-theme', nuevo);
            try { localStorage.setItem('ga-theme', nuevo); } catch (e) {}
            pintarIcono();
        });
    }

    // ---- Enlace activo en la barra de navegacion ----
    document.querySelectorAll('.ga-navbar .nav-link[href]').forEach(function (a) {
        var ruta = a.getAttribute('href');
        if (ruta && ruta !== '#' && (location.pathname === ruta || (ruta.length > 1 && location.pathname.indexOf(ruta) === 0))) {
            a.classList.add('active');
        }
    });

    // ---- Modales de crear / editar: se rellenan con los data-* del boton que los abre ----
    document.querySelectorAll('.modal[data-ga-form]').forEach(function (modal) {
        modal.addEventListener('show.bs.modal', function (ev) {
            var btn = ev.relatedTarget;
            var form = modal.querySelector('form');
            var editando = !!(btn && btn.dataset && btn.dataset.id);
            form.reset();
            var titulo = modal.querySelector('.modal-title-text');
            if (titulo) titulo.textContent = editando ? modal.dataset.tituloEditar : modal.dataset.tituloNuevo;

            form.querySelectorAll('[name]').forEach(function (el) {
                if (el.name === '_csrf' || el.hasAttribute('data-keep')) return;
                var valor = editando ? btn.dataset[el.name] : undefined;
                if (el.type === 'checkbox') {
                    el.checked = editando ? valor === 'true' : el.dataset.defecto === 'true';
                } else {
                    el.value = (valor !== undefined) ? valor : (el.dataset.defecto || '');
                }
            });

            var pw = form.querySelector('[name=password]');
            if (pw) {
                pw.required = !editando;
                var ayuda = form.querySelector('.pw-ayuda');
                if (ayuda) ayuda.style.display = editando ? 'block' : 'none';
            }
            var rol = form.querySelector('[name=rol]');
            if (rol) rol.dispatchEvent(new Event('change'));
        });
    });

    // ---- Usuarios: el selector de jugador solo aparece para el rol NOMINADO ----
    var selRol = document.querySelector('#modalUsuario [name=rol]');
    if (selRol) {
        selRol.addEventListener('change', function () {
            var campo = document.getElementById('campoJugador');
            var sel = document.querySelector('#modalUsuario [name=jugadorId]');
            var esNominado = selRol.value === 'NOMINADO';
            campo.style.display = esNominado ? 'block' : 'none';
            sel.required = esNominado;
            if (!esNominado) sel.value = '';
        });
    }

    // ---- Login: cuentas de demostracion con un clic ----
    document.querySelectorAll('[data-demo-user]').forEach(function (chip) {
        chip.addEventListener('click', function () {
            document.getElementById('username').value = chip.dataset.demoUser;
            document.getElementById('password').value = chip.dataset.demoPass;
        });
    });
});
