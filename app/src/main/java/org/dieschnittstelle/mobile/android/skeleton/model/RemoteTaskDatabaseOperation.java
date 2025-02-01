package org.dieschnittstelle.mobile.android.skeleton.model;

import android.util.Log;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public class RemoteTaskDatabaseOperation implements ITaskDatabaseOperation {
    public interface ToDoRESTWebAPI {
        @POST("/api/todos")
        Call<Task> createTask(@Body Object task);

        @GET("/api/todos")
        Call<List<Task>> readAllTasks();

        @GET("/api/todos/{todoId}")
        Call<Task> readTask(@Path("todoId") long id);

        @PUT("/api/todos/{todoId}")
        Call<Task> updateTask(@Path("todoId") long id, @Body Object task);

        @DELETE("/api/todos")
        Call<Boolean> deleteAllTasks();

        @DELETE("/api/todos/{todoId}")
        Call<Boolean> deleteTask(@Path("todoId") long id);

        @PUT("/api/users/auth")
        Call<Boolean> authenticateUser(@Body User user);

        @PUT("/api/users/prepare")
        Call<Boolean> prepare(@Body User user);
    }

    private final ToDoRESTWebAPI toDoRESTWebAPI;

    // remote DB does not have priority field. exclude priority field from JSON de-/serialization
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Exclude {}

    private static final ExclusionStrategy priorityExclusion = new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes field) {
            return field.getDeclaringClass() == Task.class && field.getName().equals("priority");
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    };
    private static final Gson gson = new GsonBuilder().setExclusionStrategies(priorityExclusion).create();

    public RemoteTaskDatabaseOperation() {
        Retrofit retrofitBuilder = new Retrofit.Builder()
                // Android emulator
                .baseUrl("http://10.0.2.2:8080")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        toDoRESTWebAPI = retrofitBuilder.create(ToDoRESTWebAPI.class);
    }

    @Override
    public Task createTask(Task task) {
        try {
            String taskJson = gson.toJson(task);
            Object taskObject = gson.fromJson(taskJson, Object.class);
            return toDoRESTWebAPI.createTask(taskObject).execute().body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Task> readAllTasks() {
        try {
            return toDoRESTWebAPI.readAllTasks().execute().body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Task readTask(long id) {
        try {
            return toDoRESTWebAPI.readTask(id).execute().body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean updateTask(Task task) {
        try {
            String taskJson = gson.toJson(task);
            Object taskObject = gson.fromJson(taskJson, Object.class);
            toDoRESTWebAPI.updateTask(task.getId(), taskObject).execute().body();
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteAllTasks() {
        try {
            return Boolean.TRUE.equals(toDoRESTWebAPI.deleteAllTasks().execute().body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deleteTask(long id) {
        try {
            return Boolean.TRUE.equals(toDoRESTWebAPI.deleteTask(id).execute().body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean authenticateUser(User user) {

        try {
            return Boolean.TRUE.equals(toDoRESTWebAPI.authenticateUser(user).execute().body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean prepare(User user) {
        Log.i("DBAuth prepare", user.getEmail() + " " + user.getPwd());
        try {
            return Boolean.TRUE.equals(toDoRESTWebAPI.prepare(user).execute().body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
