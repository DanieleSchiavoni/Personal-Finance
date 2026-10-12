// UtenteCorrenteArgumentResolver.java
package org.elis.progettoesempio.security;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import lombok.RequiredArgsConstructor;

/** Riempie i parametri annotati con @UtenteCorrente */
@Component
@RequiredArgsConstructor
public class UtenteCorrenteArgumentResolver implements HandlerMethodArgumentResolver {

    private final UtenteCorrenteProvider provider;

    @Override
    public boolean supportsParameter(MethodParameter parametro) {
        return parametro.hasParameterAnnotation(UtenteCorrente.class)
                && Long.class.equals(parametro.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parametro, ModelAndViewContainer contenitore,
            NativeWebRequest richiesta, WebDataBinderFactory fabbrica) {
        return provider.idUtenteCorrente();
    }
}