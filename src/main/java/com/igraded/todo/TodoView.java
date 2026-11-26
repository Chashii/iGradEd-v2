package com.igraded.todo;

/**
 * One line of the To-do card. detail is the small grey text, e.g. "Due Fri, Nov 27 · Assigned by Maria Cruz".
 * assigned tasks can be ticked off but not deleted by the teacher.
 */
public record TodoView(Long id, String title, String detail, boolean done, boolean assigned, boolean overdue) {
}
