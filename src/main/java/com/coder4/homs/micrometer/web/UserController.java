package com.coder4.homs.micrometer.web;


import com.coder4.homs.micrometer.web.data.UserVO;
import com.coder4.homs.micrometer.web.exception.HttpBadRequestExcepiton;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@RestController
public class UserController {

    @Autowired
    private MeterRegistry meterRegistry;

    private Counter COUNTER_GET_USER;

    @PostConstruct
    public void init() {
        COUNTER_GET_USER = meterRegistry.counter("app_requests_method_count", "method", "UserController.getUser");
    }

    @GetMapping(path = "/users/{id}")
    public UserVO getUser(@PathVariable int id) {

        if (id == -10) {
            throw new RuntimeException();
        }

        if (id <= 0) {
            throw new HttpBadRequestExcepiton();
        }

        UserVO user = new UserVO();
        user.setId(id);
        user.setName(String.format("user_%d", id));

        COUNTER_GET_USER.increment();
        return user;
    }

}
