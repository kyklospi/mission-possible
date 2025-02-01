package org.dieschnittstelle.mobile.android.skeleton.viewmodel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.dieschnittstelle.mobile.android.skeleton.model.ITaskDatabaseOperation;
import org.dieschnittstelle.mobile.android.skeleton.model.LocalTaskDatabaseOperation;
import org.dieschnittstelle.mobile.android.skeleton.model.RemoteTaskDatabaseOperation;
import org.dieschnittstelle.mobile.android.skeleton.model.Task;
import org.dieschnittstelle.mobile.android.skeleton.util.DateConverter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskListViewModel extends ViewModel {
    private static final List<Task> taskList = new ArrayList<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final MutableLiveData<ProcessingState> processingState = new MutableLiveData<>();
    private ITaskDatabaseOperation taskDbOperation;
    private LocalTaskDatabaseOperation localDatabase;
    private RemoteTaskDatabaseOperation remoteDatabase;
    private Context applicationContext;
    private Comparator<Task> currentSorter = SortOrder.SORT_BY_COMPLETED_AND_NAME.value;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    /**
     * Sets the task database operation to be used by the ViewModel.
     *
     * @param dbOp The task database operation to use.
     */
    public void setTaskDbOperation(ITaskDatabaseOperation dbOp) {
        taskDbOperation = dbOp;
    }

    /**
     * Gets the current list of tasks.
     *
     * @return The list of tasks.
     */
    public List<Task> getTaskList() {
        return taskList;
    }

    /**
     * Gets the LiveData object representing the processing state of the ViewModel.
     *
     * @return The LiveData object for the processing state.
     */
    public MutableLiveData<ProcessingState> getProcessingState() {
        return processingState;
    }

    /**
     * Sets the current sorter to be used for sorting the task list.
     *
     * @param sorter The sorter to use.
     */
    public void setCurrentSorter(SortOrder sorter) {
        currentSorter = sorter.value;
    }

    /**
     * Sets the application context for the ViewModel.
     *
     * @param ctx The application context.
     */
    public void setContext(Context ctx) {
        applicationContext = ctx;
    }

    /**
     * Sets the task database operation to the local database.
     */
    public void setLocalTaskDatabaseOperation() {
        taskDbOperation = localDatabase;
    }

    /**
     * Sets the task database operation to the remote database.
     */
    public void setRemoteTaskDatabaseOperation() {
        taskDbOperation = remoteDatabase;
    }

    /**
     * Initializes the local and remote databases.
     */
    public void initializeDb() {
        localDatabase = new LocalTaskDatabaseOperation(this.applicationContext);
        remoteDatabase = new RemoteTaskDatabaseOperation();
    }

    /**
     * Creates a new task and adds it to the database.
     *
     * @param task The task to create.
     */
    public void createTask(Task task) {
        processingState.setValue(ProcessingState.RUNNING_LONG);

        executorService.execute(() -> {
            try {
                Task createdTask = localDatabase.createTask(task);
                mainThreadHandler.post(() -> {
                    taskList.add(createdTask);
                    taskList.sort(currentSorter);
                    processingState.postValue(ProcessingState.DONE);
                });
                // update the remote db with created task
                remoteDatabase.createTask(task);
            } catch (Exception e) {
                // we assume that localdb operations are always successful
                processingState.postValue(ProcessingState.CREATE_FAIL);
            }
        });
    }

    /**
     * Reads all tasks from the database and updates the task list.
     */
    public void readAllTasks() {
        processingState.setValue(ProcessingState.RUNNING_LONG);

        executorService.execute(() -> {
            try {
                // reading from the current taskDbOperation db, could be Remote or Local
                List<Task> tasks = taskDbOperation.readAllTasks();
                mainThreadHandler.post(() -> {
                    taskList.addAll(tasks);
                    taskList.sort(currentSorter);
                    processingState.postValue(ProcessingState.DONE);
                });
            } catch (Exception e) {
                processingState.postValue(ProcessingState.READ_FAIL);
            }
        });
    }

    /**
     * Updates an existing task in the database.
     *
     * @param task The task to update.
     */
    public void updateTask(Task task) {
        processingState.setValue(ProcessingState.RUNNING_LONG);

        executorService.execute(() -> {
            boolean isUpdated = localDatabase.updateTask(task);
            if (isUpdated) {
                try {
                    mainThreadHandler.post(() -> {
                        // update the taskList model with new task
                        taskList.removeIf(t -> t.getId() == task.getId());
                        taskList.add(task);
                        taskList.sort(currentSorter);
                        processingState.postValue(ProcessingState.DONE);
                    });

                    // update the remote db with updated task
                    remoteDatabase.updateTask(task);
                } catch (Exception e) {
                    processingState.postValue(ProcessingState.UPDATE_REMOTE_FAIL);
                }
            } else {
                processingState.postValue(ProcessingState.UPDATE_LOCAL_FAIL);
            }
        });
    }

    /**
     * Deletes a task from the database.
     *
     * @param id The ID of the task to delete.
     */
    public void deleteTask(long id) {
        processingState.setValue(ProcessingState.RUNNING_LONG);

        executorService.execute(() -> {
            boolean isDeleted = localDatabase.deleteTask(id);
            if (isDeleted) {
                try {
                    mainThreadHandler.post(() -> {
                        // update the taskList model with the removed task
                        taskList.removeIf(t -> t.getId() == id);
                        processingState.postValue(ProcessingState.DONE);
                    });

                    // delete the task from the remote db
                    remoteDatabase.deleteTask(id);
                } catch (Exception ignored) {
                    processingState.postValue(ProcessingState.DELETE_REMOTE_FAIL);
                }
            } else {
                processingState.postValue(ProcessingState.DELETE_LOCAL_FAIL);
            }
        });
    }

    /**
     * Deletes all tasks from the local database.
     */
    public void deleteAllTasksFromLocal() {
        processingState.setValue(ProcessingState.RUNNING_LONG);

        executorService.execute(() -> {
            boolean isSuccess = localDatabase.deleteAllTasks();
            if (isSuccess && taskDbOperation instanceof LocalTaskDatabaseOperation) {
                mainThreadHandler.post(() -> {
                    taskList.clear();
                    processingState.postValue(ProcessingState.DONE);
                });
            } else if (!isSuccess) {
                processingState.postValue(ProcessingState.DELETE_LOCAL_FAIL);
            }
        });
    }

    /**
     * Deletes all tasks from the remote database.
     */
    public void deleteAllTasksFromRemote() {
        processingState.setValue(ProcessingState.RUNNING_LONG);

        executorService.execute(() -> {

            boolean isSuccess = remoteDatabase.deleteAllTasks();
            if (isSuccess && taskDbOperation instanceof RemoteTaskDatabaseOperation) {
                mainThreadHandler.post(() -> {
                    taskList.clear();
                    processingState.postValue(ProcessingState.DONE);
                });
            } else if (!isSuccess) {
                processingState.postValue(ProcessingState.DELETE_REMOTE_FAIL);
            }
        });
    }

    /**
     * Converts a date in milliseconds to a formatted date string.
     *
     * @param expiry The date in milliseconds.
     * @return The formatted date string.
     */
    public String toDueDateString(Long expiry) {
        String dateTime = DateConverter.toDateString(expiry);
        if (dateTime.isBlank()) {
            return dateTime;
        }
        return dateTime.split(" ")[0];
    }

    /**
     * Checks if a given date is expired.
     *
     * @param expiry The date in milliseconds.
     * @return True if the date is expired, false otherwise.
     */
    public boolean isExpiredDate(Long expiry) {
        return expiry != null && expiry <= DateConverter.fromDate(Calendar.getInstance().getTime());
    }

    /**
     * Sorts the tasks by completed status and then by name.
     */
    public void sortTasksByCompletedAndName() {
        processingState.setValue(ProcessingState.RUNNING);
        setCurrentSorter(SortOrder.SORT_BY_COMPLETED_AND_NAME);
        taskList.sort(currentSorter);
        processingState.postValue(ProcessingState.DONE);
    }

    /**
     * Sorts the tasks by priority and then by date.
     */
    public void sortTasksByPrioAndDate() {
        processingState.setValue(ProcessingState.RUNNING);
        setCurrentSorter(SortOrder.SORT_BY_PRIO_AND_DATE);
        taskList.sort(currentSorter);
        processingState.postValue(ProcessingState.DONE);
    }

    /**
     * Sorts the tasks by date and then by priority.
     */
    public void sortTasksByDateAndPrio() {
        processingState.setValue(ProcessingState.RUNNING);
        setCurrentSorter(SortOrder.SORT_BY_DATE_AND_PRIO);
        taskList.sort(currentSorter);
        processingState.postValue(ProcessingState.DONE);
    }

    /**
     * Synchronizes the local and remote databases.
     * If there are no local tasks, all tasks are transmitted from the remote to the local database.
     * If there are local tasks, then all tasks on the remote database are deleted and the local tasks are transferred to the remote database.
     */
    public void synchronizeDb() {
        processingState.setValue(ProcessingState.RUNNING_LONG);
        executorService.execute(() -> {
            try {
                List<Task> localTasks = localDatabase.readAllTasks();
                List<Task> remoteTasks = remoteDatabase.readAllTasks();

                // synchronize logic
                if (localTasks.isEmpty()) {
                    remoteTasks.forEach(task -> localDatabase.createTask(task));
                } else {
                    remoteDatabase.deleteAllTasks();
                    localTasks.forEach(task -> remoteDatabase.createTask(task));
               }

                mainThreadHandler.post(() -> {
                    // update the model tasklist
                    taskList.addAll(localTasks.isEmpty() ? remoteTasks : localTasks);
                    taskList.sort(currentSorter);
                    processingState.postValue(ProcessingState.DONE);
                });
            } catch (Exception e) {
                processingState.postValue(ProcessingState.CONNECT_REMOTE_FAIL);
                if (Objects.requireNonNull(e.getMessage()).contains("unexpected end of stream")){
                    Log.e("error", "check data in between client and server, there is a mismatch");
                }
            }
        });

    }

    public enum SortOrder {
        SORT_BY_COMPLETED_AND_NAME(
                Comparator.comparing(Task::isCompleted)
                        .thenComparing(Task::getName)
        ),
        SORT_BY_PRIO_AND_DATE(
                Comparator.comparing(Task::getPriority)
                        .thenComparingLong(Task::getExpiry)
        ),
        SORT_BY_DATE_AND_PRIO(
                Comparator.comparingLong(Task::getExpiry)
                        .thenComparing(Task::getPriority)
        );

        private final Comparator<Task> value;

        SortOrder(Comparator<Task> val) {
            value = val;
        }
    }

    public enum ProcessingState {
        CREATE_FAIL, READ_FAIL, UPDATE_REMOTE_FAIL, UPDATE_LOCAL_FAIL, DELETE_REMOTE_FAIL, DELETE_LOCAL_FAIL, CONNECT_REMOTE_FAIL, RUNNING_LONG, RUNNING, DONE, CREATE_LOCAL_SUCCESS, DELETE_LOCAL_SUCCESS, UPDATE_LOCAL_SUCCESS
    }
}
