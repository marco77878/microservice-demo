package com.marco.gateway.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello(){
        return "Hello Spring Cloud Gateway";
    }

    @GetMapping("/monoHello")
    public Mono<String> monoHello() {
        return Mono.just("Mono Hello Spring Cloud Gateway");
    }


}