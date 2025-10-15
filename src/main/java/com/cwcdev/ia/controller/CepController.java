package com.cwcdev.ia.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.cwcdev.ia.model.Endereco;
import com.cwcdev.ia.service.ViaCepService;

@Controller
public class CepController {

    @Autowired
    private ViaCepService viaCepService;

    // Lista para manter o histórico (em aplicação real, use banco de dados)
    private List<Endereco> historicoGeral = new ArrayList<>();

    @PostMapping("/buscar-cep")
    public String buscarPorCep(@RequestParam String cep, Model model) {
        Endereco endereco = viaCepService.buscarEnderecoPorCep(cep);
        
        // Adiciona ao histórico se não for erro
        if (!endereco.isErro()) {
            historicoGeral.add(0, endereco); // Adiciona no início
            // Mantém apenas os últimos 5 no histórico
            if (historicoGeral.size() > 5) {
                historicoGeral = historicoGeral.subList(0, 5);
            }
        }
        
        model.addAttribute("endereco", endereco);
        model.addAttribute("cepPesquisado", cep);
        model.addAttribute("historico", historicoGeral);
        model.addAttribute("modoBusca", "cep");
        
        return "index";
    }

    @PostMapping("/buscar-endereco")
    public String buscarPorEndereco(
            @RequestParam String uf,
            @RequestParam String localidade,
            @RequestParam String logradouro,
            Model model) {
        
        Endereco endereco = viaCepService.buscarEnderecoPorLogradouro(uf, localidade, logradouro);
        
        // Adiciona ao histórico se não for erro
        if (!endereco.isErro()) {
            historicoGeral.add(0, endereco); // Adiciona no início
            // Mantém apenas os últimos 5 no histórico
            if (historicoGeral.size() > 5) {
                historicoGeral = historicoGeral.subList(0, 5);
            }
        }
        
        model.addAttribute("endereco", endereco);
        model.addAttribute("ufPesquisado", uf);
        model.addAttribute("localidadePesquisada", localidade);
        model.addAttribute("logradouroPesquisado", logradouro);
        model.addAttribute("historico", historicoGeral);
        model.addAttribute("modoBusca", "endereco");
        
        return "index";
    }

    @GetMapping("/limpar")
    public String limpar(Model model) {
        historicoGeral.clear();
        model.addAttribute("endereco", new Endereco());
        model.addAttribute("historico", historicoGeral);
        return "index";
    }
}