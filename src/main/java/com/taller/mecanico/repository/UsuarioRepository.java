package com.taller.mecanico.repository;

import com.taller.mecanico.entity.Usuario;
import com.taller.mecanico.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRolAndActivoTrue(Rol rol);

    boolean existsByRol(Rol rol);

    List<Usuario> findAllByOrderByActivoDescNombreCompletoAsc();
}
