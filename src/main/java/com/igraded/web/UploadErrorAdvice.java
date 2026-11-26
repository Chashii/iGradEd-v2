package com.igraded.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Spring rejects a too-large upload before the controller runs. Instead of an error page,
 * send the teacher back to the Locker with a message.
 */
@ControllerAdvice
public class UploadErrorAdvice {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String tooBig() {
        return "redirect:/locker?tooBig";
    }
}
