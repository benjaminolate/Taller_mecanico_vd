package com.taller.mecanico.config;

import com.taller.mecanico.enums.Rol;
import com.taller.mecanico.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Asegura que existan las dos cuentas del sistema: una de jefe y una de empleado.
 * Si falta alguna (por ejemplo, la primera vez que arranca), la crea como "jefe"
 * o "empleado" con la contraseña inicial configurada. Después el jefe puede
 * cambiar el nombre de usuario y la contraseña de ambas desde "Cuentas"; por eso
 * se revisa por rol y no por nombre.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UsuarioInicial implements ApplicationRunner {

    public static final String JEFE = "jefe";
    public static final String EMPLEADO = "empleado";

    private final UsuarioService usuarioService;

    @Value("${taller.admin.password:}")
    private String passwordJefe;

    @Value("${taller.empleado.password:}")
    private String passwordEmpleado;

    @Override
    public void run(ApplicationArguments args) {
        crearSiFalta(JEFE, "Jefe del taller", Rol.JEFE, passwordJefe);
        crearSiFalta(EMPLEADO, "Empleados del taller", Rol.EMPLEADO,
                StringUtils.hasText(passwordEmpleado) ? passwordEmpleado : passwordJefe);
    }

    private void crearSiFalta(String username, String nombre, Rol rol, String password) {
        if (usuarioService.existeCuentaConRol(rol)) {
            return;
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalStateException("Falta la cuenta '" + username + "' y no se definió su contraseña "
                    + "inicial. Define la variable de entorno ADMIN_PASSWORD (o taller.admin.password en "
                    + "application-local.properties) y vuelve a iniciar.");
        }
        usuarioService.crear(username, nombre, rol, password);
        log.warn("Se creó la cuenta '{}' ({}) con la contraseña inicial configurada.", username, rol);
    }
}
