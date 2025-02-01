package org.dieschnittstelle.mobile.android.skeleton.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.dieschnittstelle.mobile.android.skeleton.model.ITaskDatabaseOperation;
import org.dieschnittstelle.mobile.android.skeleton.model.User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Pattern;

public class MainViewModel extends ViewModel {
    private ITaskDatabaseOperation taskDBOperation;
    private final MutableLiveData<DatabaseState> databaseState = new MutableLiveData<>();
    private final MutableLiveData<LoginState> loginState = new MutableLiveData<>();
    private final MutableLiveData<String> mailInputError = new MutableLiveData<>();
    private final MutableLiveData<String> passwordInputError = new MutableLiveData<>();
    private final Handler emailAdressValidationHandler = new Handler(Looper.getMainLooper());
    private final Handler passwordAdressValidationHandler = new Handler(Looper.getMainLooper());
    private boolean lastMailInputOk = false;
    private boolean lastPasswordInputOk = false;
    private User user;

    public void authenticateUser() {
        if (isValidEMail() && isSixDigitPwd()) {
            loginState.setValue(LoginState.RUNNING);
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if (taskDBOperation.authenticateUser(user)) {
                    loginState.postValue(LoginState.AUTHENTICATION_SUCCESS);
                } else {
                    loginState.postValue(LoginState.AUTHENTICATION_FAIL);
                }
            }).start();
        }
    }

    public boolean isValidEMail() {
        if (user.getEmail() != null) {
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(user.getEmail()).matches()) {
                lastMailInputOk = true;
                return true;
            }
            this.mailInputError.setValue("Input not valid, must be an e-mail address");
            lastMailInputOk = false;
            return false;
        }
        return false;
    }

    public boolean isSixDigitPwd() {
        if (user.getPwd() != null) {
            boolean sixDigitPattern = Pattern.matches("[0-9]{6}", user.getPwd());
            if (sixDigitPattern) {
                lastPasswordInputOk = true;
                return true;
            }
            this.passwordInputError.setValue("Input not valid, must consist of 6 numbers");
            return false;
        }
        return false;
    }

    public boolean onMailInputChanged() {
        new Thread(() -> {
            loginState.postValue(LoginState.RESET);
            mailInputError.postValue(null);
            emailAdressValidationHandler.removeCallbacksAndMessages(null); // Remove pending validations
            emailAdressValidationHandler.postDelayed(() -> {
                if (isValidEMail()) {
                    mailInputError.postValue(null); // Clear any previous error
                    if (lastMailInputOk && lastPasswordInputOk) {
                        loginState.postValue(LoginState.READY);
                    }
                } else {
                    mailInputError.postValue("Input not valid, must be an e-mail address");
                }
            }, 2000);
        }).start();
        return true;
    }

    public boolean onPasswordInputChanged() {
        new Thread(() -> {
            loginState.postValue(LoginState.RESET);
            passwordInputError.postValue(null);
            passwordAdressValidationHandler.removeCallbacksAndMessages(null); // Remove pending validations
            passwordAdressValidationHandler.postDelayed(() -> {
                if (isSixDigitPwd()) {
                    passwordInputError.postValue(null); // Clear any previous error
                    if (lastMailInputOk && lastPasswordInputOk) {
                        loginState.postValue(LoginState.READY);
                    }
                } else {
                    passwordInputError.postValue("Input not valid, must consist of 6 numbers");
                }
            }, 2000);
        }).start();
        return true;
    }

    public MutableLiveData<DatabaseState> getDatabaseState() {
        return databaseState;
    }

    public MutableLiveData<LoginState> getLoginState() {
        return loginState;
    }

    public MutableLiveData<String> getMailInputError() {
        return mailInputError;
    }

    public MutableLiveData<String> getPasswordInputError() {
        return passwordInputError;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setTaskDBOperation(ITaskDatabaseOperation taskDBOperation) {
        this.taskDBOperation = taskDBOperation;
    }

    public void checkRemoteTaskDatabaseOperation() {
        new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress("10.0.2.2", 8080), 500);
                socket.close();
                databaseState.postValue(DatabaseState.CONNECT_REMOTE_SUCCESS);
            } catch (IOException e) {
                databaseState.postValue(DatabaseState.CONNECT_REMOTE_FAIL);
            }
        }).start();
    }

    public enum DatabaseState {
        CONNECT_REMOTE_SUCCESS, CONNECT_REMOTE_FAIL
    }

    public enum LoginState {
        AUTHENTICATION_SUCCESS, AUTHENTICATION_FAIL, RUNNING, READY, RESET
    }
}
