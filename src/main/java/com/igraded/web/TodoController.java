package com.igraded.web;

import java.time.LocalDate;
import java.util.NoSuchElementException;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.igraded.security.AppUserDetails;
import com.igraded.todo.TodoService;

/** The To-do card on the home page. Every action only touches the logged-in person's own tasks. */
@Controller
public class TodoController {

    private final TodoService todos;

    public TodoController(TodoService todos) {
        this.todos = todos;
    }

    @PostMapping("/todos")
    public String add(@AuthenticationPrincipal AppUserDetails me,
                      @RequestParam String title,
                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate due,
                      RedirectAttributes redirect) {
        try {
            todos.addPersonal(me.getId(), title, due);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("todoError", e.getMessage());
        }
        return "redirect:/";
    }

    /** Called by the page's script when a box is ticked or unticked. Answers 204 with no body. */
    @PostMapping("/todos/{id}/done")
    public ResponseEntity<Void> done(@AuthenticationPrincipal AppUserDetails me,
                                     @PathVariable Long id,
                                     @RequestParam boolean done) {
        try {
            todos.setDone(me.getId(), id, done);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/todos/{id}/delete")
    public String delete(@AuthenticationPrincipal AppUserDetails me, @PathVariable Long id) {
        try {
            if (!todos.deletePersonal(me.getId(), id)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tasks given by an admin can't be deleted");
            }
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return "redirect:/";
    }
}
