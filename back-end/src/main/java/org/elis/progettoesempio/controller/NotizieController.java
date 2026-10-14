// controller/NotizieController.java
package org.elis.progettoesempio.controller;

import java.util.List;

import org.elis.progettoesempio.dto.response.NotiziaResponse;
import org.elis.progettoesempio.service.NotizieService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** Pubblico: le notizie si vedono anche senza accedere */
@RestController
@RequestMapping("/api/notizie")
@RequiredArgsConstructor
public class NotizieController {

    private final NotizieService notizieService;

    @GetMapping
    public List<NotiziaResponse> ultime(@RequestParam(defaultValue = "30") int limite) {
        return notizieService.ultime(limite);
    }
}