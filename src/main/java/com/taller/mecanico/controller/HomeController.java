package com.taller.mecanico.controller;

import com.taller.mecanico.service.PanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Panel de inicio: lo que hay que atender hoy en el taller.
 * El resumen por mecánico solo se calcula y muestra para el jefe.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final PanelService panelService;
    private final com.taller.mecanico.repository.MecanicoRepository mecanicoRepository;

    @GetMapping("/")
    public String inicio(Model model, Authentication auth) {
        model.addAttribute("conteo",          panelService.conteoPorEstado());
        model.addAttribute("ingresadasMes",   panelService.ingresadasEsteMes());
        model.addAttribute("listas",          panelService.listasParaEntregar());
        model.addAttribute("atrasadas",       panelService.atrasadas());
        model.addAttribute("sinMecanico",     panelService.sinMecanico());
        model.addAttribute("mecanicosActivos", mecanicoRepository.findByEstadoActivoTrueOrderByNombreAsc());
        model.addAttribute("diasAtraso",      PanelService.DIAS_ATRASO);

        boolean esJefe = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_JEFE".equals(a.getAuthority()));
        if (esJefe) {
            model.addAttribute("resumenMecanicos", panelService.resumenMecanicosDelMes());
        }
        return "index";
    }
}
