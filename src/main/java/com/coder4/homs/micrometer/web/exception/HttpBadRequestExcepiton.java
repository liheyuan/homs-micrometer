package com.coder4.homs.micrometer.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class HttpBadRequestExcepiton extends RuntimeException {
}
