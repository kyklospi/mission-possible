package org.dieschnittstelle.mobile.android.skeleton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;

import org.dieschnittstelle.mobile.android.skeleton.databinding.ActivityMainBinding;
import org.dieschnittstelle.mobile.android.skeleton.model.ITaskDatabaseOperation;
import org.dieschnittstelle.mobile.android.skeleton.model.User;
import org.dieschnittstelle.mobile.android.skeleton.viewmodel.MainViewModel;

public class MainActivity extends AppCompatActivity {
    private MainViewModel viewModel;
    private ProgressBar progressBar;
    private User user;
    private Snackbar snackbar;
    private Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        this.viewModel.getDatabaseState().observe(this, this::handleDatabaseState);
        this.viewModel.getLoginState().observe(this, this::handleUserLoginState);
        this.viewModel.checkRemoteTaskDatabaseOperation();
        if (user == null) {
            user = new User();
        }
        viewModel.setUser(user);
    }

    private void handleDatabaseState(MainViewModel.DatabaseState databaseState) {
        if (databaseState != null) {
            switch (databaseState) {
                case CONNECT_REMOTE_FAIL:
                    callTaskListViewIntent();
                    break;
                case CONNECT_REMOTE_SUCCESS:
                    ActivityMainBinding MainViewBinding = DataBindingUtil.setContentView(
                            this,
                            R.layout.activity_main
                    );
                    MainViewBinding.setMainViewModel(this.viewModel);
                    MainViewBinding.setLifecycleOwner(this);

                    ITaskDatabaseOperation taskDBOperation = ((TaskApplication) getApplication()).getTaskDatabaseOperation();
                    viewModel.setTaskDBOperation(taskDBOperation);

                    TextView welcomeText = findViewById(R.id.welcomeText);
                    welcomeText.setText(R.string.welcome_message);

                    progressBar = findViewById(R.id.progressBar);
                    progressBar.setVisibility(View.GONE);
                    loginButton = findViewById(R.id.loginButton);
                    loginButton.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
        }
    }

    private void handleUserLoginState(MainViewModel.LoginState loginState) {
        if (loginState != null) {
            switch (loginState) {
                case RESET:
                    progressBar.setVisibility(View.GONE);
                    loginButton.setVisibility(View.GONE);
                    if (snackbar != null) {
                        snackbar.dismiss();
                    }
                    break;
                case READY:
                    loginButton.setVisibility(View.VISIBLE);
                    break;
                case RUNNING:
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case AUTHENTICATION_FAIL:
                    progressBar.setVisibility(View.GONE);
                    showPersistentMessage(getString(R.string.login_authentication_failed));
                    break;
                case AUTHENTICATION_SUCCESS:
                    progressBar.setVisibility(View.GONE);
                    callTaskListViewIntent();
                    break;
                default:
                    break;
            }
        }
    }

    private void callTaskListViewIntent() {
        Intent callTaskListViewIntent = new Intent(this, TaskListViewActivity.class);
        startActivity(callTaskListViewIntent);
        this.finish();
    }

    private void showPersistentMessage(String message) {
        snackbar = Snackbar.make(findViewById(R.id.rootView), message, Snackbar.LENGTH_INDEFINITE);
        snackbar.show();
    }
}
