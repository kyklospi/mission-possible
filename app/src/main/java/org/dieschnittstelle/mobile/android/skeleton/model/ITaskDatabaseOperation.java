package org.dieschnittstelle.mobile.android.skeleton.model;

import java.util.List;

public interface ITaskDatabaseOperation {

    Task createTask(Task task);

    List<Task> readAllTasks();

    Task readTask(long id);

    boolean updateTask(Task task);

    boolean deleteAllTasks();

    boolean deleteTask(long id);

    boolean authenticateUser(User user);

    boolean prepare(User user);
}
