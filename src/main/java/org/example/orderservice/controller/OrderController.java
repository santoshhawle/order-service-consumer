package org.example.orderservice.controller;

import org.example.orderservice.model.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping("/{id}")
    public String getOrder(@PathVariable Long id) {

        RestTemplate restTemplate = new RestTemplate();

        User user = restTemplate.getForObject(
                "http://localhost:8081/users/1",
                User.class);

        return "Order " + id + " created by " + user;
    }
}