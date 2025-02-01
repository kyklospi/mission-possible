package org.dieschnittstelle.mobile.android.skeleton;

import android.app.Application;

import org.dieschnittstelle.mobile.android.skeleton.model.ITaskDatabaseOperation;
import org.dieschnittstelle.mobile.android.skeleton.model.RemoteTaskDatabaseOperation;

public class TaskApplication extends Application {

    private ITaskDatabaseOperation taskDatabaseOperation;

    @Override
    public void onCreate() {
        super.onCreate();
        // this.taskDatabaseOperation = new MockTaskDatabaseOperation();
        // this.taskDatabaseOperation = new LocalTaskDatabaseOperation(this);
        this.taskDatabaseOperation = new RemoteTaskDatabaseOperation();
    }

    public ITaskDatabaseOperation getTaskDatabaseOperation() {
        return taskDatabaseOperation;
    }

    public void setTaskDatabaseOperation(ITaskDatabaseOperation taskDatabaseOperation) {
        this.taskDatabaseOperation = taskDatabaseOperation;
    }
}
