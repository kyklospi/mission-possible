package org.dieschnittstelle.mobile.android.skeleton;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.dieschnittstelle.mobile.android.skeleton.databinding.StructuredTaskViewBinding;
import org.dieschnittstelle.mobile.android.skeleton.model.Task;
import org.dieschnittstelle.mobile.android.skeleton.viewmodel.TaskListViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TaskListViewActivity extends AppCompatActivity {
    private ArrayAdapter<Task> taskListViewAdapter;
    private ProgressBar progressBar;
    private TaskListViewModel viewModel;
    private boolean userIsInteracting;

    private final ActivityResultLauncher<Intent> taskDetailViewForEditLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), activityResult -> {
        if (activityResult.getData() != null) {
            Task task = (Task) activityResult.getData().getSerializableExtra(TaskDetailViewActivity.TASK_DETAIL_VIEW_KEY);
            if (task != null) {
                if (activityResult.getResultCode() == TaskDetailViewActivity.RESULT_OK) {
                    viewModel.updateTask(task);
                } else if (activityResult.getResultCode() == TaskDetailViewActivity.RESULT_DELETE_OK) {
                    viewModel.deleteTask(task.getId());
                }
            }
        }
    });

    private final ActivityResultLauncher<Intent> taskDetailViewForAddLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), activityResult -> {
        if (activityResult.getResultCode() == TaskDetailViewActivity.RESULT_OK && activityResult.getData() != null) {
            Task task = (Task) activityResult.getData().getSerializableExtra(TaskDetailViewActivity.TASK_DETAIL_VIEW_KEY);
            viewModel.createTask(task);
            showMessage(getString(R.string.task_added_feedback_message) + " " + (task != null ? task.getName() : ""));
        }
    });

    private final ActivityResultLauncher<Intent> taskListMapViewLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), activityResult -> {
        if (activityResult.getResultCode() == TaskListViewMapActivity.RESULT_OK && activityResult.getData() != null) {
            Task task = (Task) activityResult.getData().getSerializableExtra(TaskListViewMapActivity.TASK_LIST_VIEW_MAP_KEY);
            showEditTaskDetailView(task);
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list_view);

        viewModel = new ViewModelProvider(this).get(TaskListViewModel.class);
        viewModel.setContext(getApplicationContext());
        viewModel.setTaskDbOperation(((TaskApplication) getApplication()).getTaskDatabaseOperation());
        viewModel.getProcessingState().observe(this, this::handleTaskProcessingState);
        viewModel.initializeDb();
        viewModel.synchronizeDb();

        taskListViewAdapter = new TaskListAdapter(this, R.layout.structured_task_view, viewModel.getTaskList());
        ListView taskListView = findViewById(R.id.taskListView);
        taskListView.setAdapter(taskListViewAdapter);

        FloatingActionButton addTaskAction = findViewById(R.id.addTaskAction);
        addTaskAction.setOnClickListener(view -> showNewTaskDetailView());

        progressBar = findViewById(R.id.progressBar);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        userIsInteracting = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_task_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sortTasks) {
            showMessage("Sorting all done missions...");
            this.viewModel.sortTasksByCompletedAndName();
            return true;
        }

        if (item.getItemId() == R.id.sortTasksByPrio) {
            showMessage("Sorting all missions by priority...");
            this.viewModel.sortTasksByPrioAndDate();
            return true;
        }

        if (item.getItemId() == R.id.sortTasksByDate) {
            showMessage("Sorting all missions by date...");
            this.viewModel.sortTasksByDateAndPrio();
            return true;
        }

        if (item.getItemId() == R.id.deleteAllLocalTasks) {
            showMessage("Deleting all missions from local database...");
            this.viewModel.deleteAllTasksFromLocal();
            return true;
        }

        if (item.getItemId() == R.id.deleteAllRemoteTasks) {
            showMessage("Deleting all missions from remote database...");
            this.viewModel.deleteAllTasksFromRemote();
            return true;
        }

        if (item.getItemId() == R.id.syncLocalRemoteDB) {
            showMessage("Syncing all missions between database...");
            viewModel.synchronizeDb();
            return true;
        }

        if (item.getItemId() == R.id.showMapView) {
            showTaskMapView();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showEditTaskDetailView(Task task) {
        Intent callTaskDetailViewIntent = new Intent(this, TaskDetailViewActivity.class);
        callTaskDetailViewIntent.putExtra(TaskDetailViewActivity.TASK_DETAIL_VIEW_KEY, task);
        taskDetailViewForEditLauncher.launch(callTaskDetailViewIntent);
    }

    private void showNewTaskDetailView() {
        Intent callTaskDetailViewIntent = new Intent(this, TaskDetailViewActivity.class);
        taskDetailViewForAddLauncher.launch(callTaskDetailViewIntent);
    }

    private void showTaskMapView() {
        Intent callTaskListMapViewIntent = new Intent(this, TaskListViewMapActivity.class);
        taskListMapViewLauncher.launch(callTaskListMapViewIntent);
    }

    private void handleTaskProcessingState(TaskListViewModel.ProcessingState processingState) {
        switch (processingState) {
            case RUNNING_LONG:
                progressBar.setVisibility(View.VISIBLE);
                taskListViewAdapter.notifyDataSetChanged();
                break;
            case CREATE_FAIL:
                progressBar.setVisibility(View.GONE);
                taskListViewAdapter.notifyDataSetChanged();
                showMessage(getString(R.string.remote_db_create_fail_message));
                break;
            case UPDATE_REMOTE_FAIL:
                progressBar.setVisibility(View.GONE);
                taskListViewAdapter.notifyDataSetChanged();
                showMessage(getString(R.string.remote_db_update_fail_message));
                break;
            case UPDATE_LOCAL_FAIL:
                progressBar.setVisibility(View.GONE);
                taskListViewAdapter.notifyDataSetChanged();
                showMessage(getString(R.string.local_db_update_fail_message));
                break;
            case DELETE_REMOTE_FAIL:
                progressBar.setVisibility(View.GONE);
                taskListViewAdapter.notifyDataSetChanged();
                showMessage(getString(R.string.remote_db_delete_fail_message));
                break;
            case DELETE_LOCAL_FAIL:
                progressBar.setVisibility(View.GONE);
                taskListViewAdapter.notifyDataSetChanged();
                showMessage(getString(R.string.local_db_delete_fail_message));
                break;
            case READ_FAIL:
                progressBar.setVisibility(View.GONE);
                taskListViewAdapter.notifyDataSetChanged();
                showMessage(getString(R.string.db_read_fail_message));
                break;
            case CONNECT_REMOTE_FAIL:
                progressBar.setVisibility(View.GONE);
                taskListViewAdapter.notifyDataSetChanged();
                showMessage(getString(R.string.task_db_connect_fail_message));
                viewModel.setLocalTaskDatabaseOperation();
                viewModel.readAllTasks();
                break;
            case DONE:
                progressBar.setVisibility(View.GONE);
                taskListViewAdapter.notifyDataSetChanged();
                break;
            case CREATE_LOCAL_SUCCESS:
            case DELETE_LOCAL_SUCCESS:
            case UPDATE_LOCAL_SUCCESS:
            case RUNNING:
            default:
                break;
        }
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.taskListViewActivity), message, Snackbar.LENGTH_SHORT).show();
    }

    private void setDueDateColor(Long expiry, TextView dueDateView) {
        int color = viewModel.isExpiredDate(expiry) ? Color.RED : Color.DKGRAY;

        dueDateView.setTextColor(color);
    }

    private void setPriorityDropDown(Task task, Spinner prioritySpinner, View taskView) {
        List<String> priorities = new ArrayList<>(Arrays.asList(Task.Priority.NONE.name(), Task.Priority.LOW.name(), Task.Priority.NORMAL.name(), Task.Priority.HIGH.name(), Task.Priority.CRITICAL.name()));
        if (!task.getPriority().equals(Task.Priority.NONE)) {
            priorities.add(0, task.getPriority().name());
            priorities = priorities.stream().distinct().collect(Collectors.toList());
        }

        ArrayAdapter<String> dropDownPriorityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, priorities);

        dropDownPriorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        prioritySpinner.setAdapter(dropDownPriorityAdapter);
        prioritySpinner.setSelection(0, false);

        prioritySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {

                // this is necessary to indicate that there was a change in the spinner. the userIsInteracting
                // will also be called when the user click on sorts or any other ui components
                boolean valueChanged = task.getPriority() != Task.Priority.valueOf(prioritySpinner.getSelectedItem().toString());

                // this is necessary because of the automatic item selection event being triggered
                // when notifydatasetchanged is called, it will create a cyclic loop of updating the
                // view and re-triggering the item selection
                if (userIsInteracting && valueChanged) {
                    userIsInteracting = false;
                    viewModel.updateTask(task);
                }

                String selectedPriority = prioritySpinner.getSelectedItem().toString();
                task.setPriority(Task.Priority.valueOf(selectedPriority));
                taskView.setBackgroundResource(task.getPriority().resourceId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private class TaskListAdapter extends ArrayAdapter<Task> {
        public TaskListAdapter(Context owner, int resourceId, List<Task> taskList) {
            super(owner, resourceId, taskList);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View recyclableTaskView, @NonNull ViewGroup parent) {
            View taskView;
            StructuredTaskViewBinding taskViewBinding;
            Task taskFromList = getItem(position);

            // recyclableTaskView do not exist, then create one
            if (recyclableTaskView == null) {
                taskViewBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.structured_task_view, null, false);
                taskView = taskViewBinding.getRoot();
                taskView.setTag(taskViewBinding);
                taskViewBinding.setTaskListViewModel(viewModel);

            // recyclableTaskView exist, then reuse and access it with binding object
            } else {
                taskView = recyclableTaskView;
                taskViewBinding = (StructuredTaskViewBinding) taskView.getTag();
            }

            // the task from the list could potentially be empty.
            if (taskFromList == null) {
                return taskView;
            }

            taskViewBinding.setTask(taskFromList);
            taskView.setBackgroundResource(taskFromList.getPriority().resourceId);

            taskView.setOnClickListener(v -> {
                Task selectedTask = taskListViewAdapter.getItem(position);
                showEditTaskDetailView(selectedTask);
            });

            TextView dueDateView = taskView.findViewById(R.id.taskDate);
            setDueDateColor(taskFromList.getExpiry(), dueDateView);

            Spinner prioritySpinner = taskView.findViewById(R.id.dropdownPriority);
            setPriorityDropDown(taskFromList, prioritySpinner, taskView);

            return taskView;
        }
    }
}
