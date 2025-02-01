package org.dieschnittstelle.mobile.android.skeleton.model;

import org.dieschnittstelle.mobile.android.skeleton.util.DateConverter;

import java.util.ArrayList;
import java.util.List;

public class MockTaskDatabaseOperation implements ITaskDatabaseOperation {

    private static long idCounter = 0;
    private final List<Task> tasks = new ArrayList<>();

    public MockTaskDatabaseOperation() {
        createTask(new Task("Aufgabe 1", "Beschreibung 1", DateConverter.fromDateString("01.01.2025 12:00"), false, false, Task.Priority.CRITICAL, new ArrayList<>()));
        createTask(new Task("Aufgabe 2", "Beschreibung 2", DateConverter.fromDateString("05.12.2024 10:00"),false, false, Task.Priority.HIGH, new ArrayList<>()));
        createTask(new Task("Aufgabe 3", "Beschreibung 3", DateConverter.fromDateString("01.10.2024 09:00"),false, false, Task.Priority.NORMAL, new ArrayList<>()));
        createTask(new Task("Aufgabe 4", "Beschreibung 4", DateConverter.fromDateString("15.01.2024 08:00"),false, false, Task.Priority.LOW, new ArrayList<>()));
    }

    @Override
    public Task createTask(Task task) {
        task.setId(++idCounter);
        tasks.add(task);
        return task;
    }

    @Override
    public List<Task> readAllTasks() {
        return new ArrayList<>(tasks);
    }

    @Override
    public Task readTask(long id) {
        return tasks.stream()
                .filter(existingTask -> existingTask.getId() == (id))
                .findAny()
                .orElse(null);
    }

    @Override
    public boolean updateTask(Task task) {
        Task selectedTask = tasks.stream()
                .filter(existingTask -> existingTask.getId() == (task.getId()))
                .findAny()
                .orElse(null);
        if (selectedTask == null) {
            return false;
        }
        selectedTask.setName(task.getName());
        selectedTask.setDescription(task.getDescription());
        selectedTask.setFavorite(task.isFavorite());
        selectedTask.setCompleted(task.isCompleted());
        selectedTask.setExpiry(task.getExpiry());
        selectedTask.setPriority(task.getPriority());
        return true;
    }

    @Override
    public boolean deleteAllTasks() {
        if (!tasks.isEmpty()) {
            tasks.clear();
        }
        return true;
    }

    @Override
    public boolean deleteTask(long id) {
        tasks.removeIf(existingTask -> existingTask.getId() == (id));
        return true;
    }

    @Override
    public boolean authenticateUser(User user) {
        return false;
    }

    @Override
    public boolean prepare(User user) {
        return false;
    }
}
