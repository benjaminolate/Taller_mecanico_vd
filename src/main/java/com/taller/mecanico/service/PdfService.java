package com.taller.mecanico.service;

import com.taller.mecanico.entity.FirmaOrden;
import com.taller.mecanico.entity.OrdenIngreso;
import com.taller.mecanico.enums.MomentoFirma;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Genera un PDF a partir de la plantilla Thymeleaf "ordenes/orden-pdf.html".
 * Usa Flying Saucer (xhtmlrenderer) que convierte XHTML+CSS → PDF.
 */
@Service
@RequiredArgsConstructor
public class PdfService {

    private final TemplateEngine templateEngine;
    private final FirmaService   firmaService;

    public byte[] generarPdfOrden(OrdenIngreso orden) {
        try {
            // Preparar el contexto de Thymeleaf con los datos de la orden
            Context context = new Context();
            context.setVariable("orden", orden);
            Map<MomentoFirma, FirmaOrden> firmas = firmaService.vigentes(orden.getId());
            context.setVariable("firmaRecepcion", firmas.get(MomentoFirma.RECEPCION));
            context.setVariable("firmaRetiro", firmas.get(MomentoFirma.RETIRO));

            // Procesar la plantilla HTML → String XHTML
            String html = templateEngine.process("ordenes/orden-pdf", context);

            // Convertir el XHTML a PDF con Flying Saucer
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF para la orden " + orden.getId(), e);
        }
    }
}
