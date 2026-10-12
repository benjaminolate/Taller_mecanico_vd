package com.taller.mecanico.specification;

import com.taller.mecanico.dto.OrdenBusquedaDTO;
import com.taller.mecanico.entity.Cliente;
import com.taller.mecanico.entity.Mecanico;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.entity.TrabajoRealizado;
import com.taller.mecanico.entity.Vehiculo;
import com.taller.mecanico.util.Formatos;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye el predicado combinado de búsqueda para OrdenIngreso a partir de
 * un {@link OrdenBusquedaDTO}. Cada criterio se activa solo si el usuario lo
 * llenó (StringUtils.hasText / != null), por lo que cualquier combinación de
 * filtros es válida.
 *
 * Notas de diseño / rendimiento:
 * <ul>
 *   <li>Todos los joins son LEFT JOIN: una orden de tipo CREMALLERA sin
 *       vehículo, o sin trabajos registrados aún, no debe quedar excluida
 *       de resultados que no filtran por esos campos.</li>
 *   <li>Se usa {@code query.distinct(true)} porque el filtro por "mecánico"
 *       hace JOIN contra la colección {@code trabajos} (OneToMany); sin
 *       distinct, una orden con 2 trabajos del mismo mecánico aparecería
 *       duplicada. El distinct se resuelve en SQL (no en memoria), por lo
 *       que la paginación sigue siendo correcta y eficiente.</li>
 *   <li>Cliente y Vehiculo (relaciones *ToOne*) se cargan con FETCH JOIN
 *       únicamente en la query principal (no en la query de conteo que usa
 *       Pageable internamente) para evitar el problema N+1 al renderizar la
 *       lista, sin el riesgo de duplicar filas que sí tendría un fetch sobre
 *       una colección.</li>
 * </ul>
 */
public class OrdenIngresoSpecification {

    private OrdenIngresoSpecification() {
        // Clase de utilidad: no se instancia
    }

    public static Specification<OrdenIngreso> conFiltros(OrdenBusquedaDTO criterios) {
        return (root, query, cb) -> {
            boolean esQueryDeConteo = Long.class.equals(query.getResultType())
                    || long.class.equals(query.getResultType());

            query.distinct(true);

            // Fetch sobre relaciones *ToOne (evita el N+1 al renderizar la lista).
            // No se hace en la query de conteo porque Hibernate lo prohíbe ahí
            // (un fetch no tiene sentido si solo se necesita un número).
            if (!esQueryDeConteo) {
                root.fetch("cliente", JoinType.LEFT);
                root.fetch("vehiculo", JoinType.LEFT);
                root.fetch("mecanico", JoinType.LEFT);
                root.fetch("recibidoPor", JoinType.LEFT);
            }

            // Joins independientes para los predicados. Hibernate reutiliza el JOIN SQL
            // ya generado por el fetch de arriba sobre el mismo atributo, así que esto
            // no duplica el join en la query final.
            Join<OrdenIngreso, Cliente> clienteJoin = root.join("cliente", JoinType.LEFT);
            Join<OrdenIngreso, Vehiculo> vehiculoJoin = root.join("vehiculo", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();

            String patente = Formatos.normalizarPatente(criterios.getPatente());
            if (patente != null) {
                // Las patentes se guardan normalizadas (mayúsculas, sin guiones)
                predicates.add(cb.like(vehiculoJoin.get("patente"), "%" + patente + "%"));
            }
            if (StringUtils.hasText(criterios.getClienteNombre())) {
                predicates.add(cb.like(
                        cb.lower(clienteJoin.get("nombre")),
                        "%" + criterios.getClienteNombre().trim().toLowerCase() + "%"));
            }
            String rut = Formatos.limpiarRut(criterios.getClienteRut());
            if (rut != null) {
                // Se compara sin puntos ni guion: "12345678" encuentra "12.345.678-5"
                Expression<String> rutSinFormato = cb.function("replace", String.class,
                        cb.function("replace", String.class, clienteJoin.get("rut"),
                                cb.literal("."), cb.literal("")),
                        cb.literal("-"), cb.literal(""));
                predicates.add(cb.like(cb.upper(rutSinFormato), "%" + rut + "%"));
            }
            if (criterios.getMecanicoId() != null) {
                Join<OrdenIngreso, TrabajoRealizado> trabajoJoin = root.join("trabajos", JoinType.LEFT);
                Join<TrabajoRealizado, Mecanico> mecanicoJoin = trabajoJoin.join("mecanico", JoinType.LEFT);
                Join<OrdenIngreso, Mecanico> responsableJoin = root.join("mecanico", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.equal(responsableJoin.get("id"), criterios.getMecanicoId()),
                        cb.equal(mecanicoJoin.get("id"), criterios.getMecanicoId())));
            }
            var estados = criterios.getVistaEfectiva().getEstados();
            if (!estados.isEmpty()) {
                predicates.add(root.get("estado").in(estados));
            }
            if (criterios.getFechaDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("fecha"), criterios.getFechaDesde().atStartOfDay()));
            }
            if (criterios.getFechaHasta() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("fecha"), criterios.getFechaHasta().atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
