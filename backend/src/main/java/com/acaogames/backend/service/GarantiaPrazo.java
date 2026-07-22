package com.acaogames.backend.service;

import com.acaogames.backend.exception.BadRequestException;

final class GarantiaPrazo {

    private GarantiaPrazo() {
    }

    static int validar(Integer dias) {
        int valor = dias == null ? 0 : dias.intValue();
        if (valor != 0 && valor != 30 && valor != 90 && valor != 365) {
            throw new BadRequestException("Prazo de garantia invalido.");
        }
        return valor;
    }

    static String descricao(Integer dias) {
        int valor = dias == null ? 0 : dias.intValue();
        if (valor == 30) {
            return "30 dias de garantia";
        }
        if (valor == 90) {
            return "90 dias de garantia";
        }
        if (valor == 365) {
            return "1 ano de garantia";
        }
        return "Sem garantia";
    }
}
