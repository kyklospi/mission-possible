package org.dieschnittstelle.mobile.android.skeleton;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import org.dieschnittstelle.mobile.android.skeleton.databinding.ActivityTaskDetailViewBinding;
import org.dieschnittstelle.mobile.android.skeleton.databinding.ContactItemViewBinding;
import org.dieschnittstelle.mobile.android.skeleton.model.Task;
import org.dieschnittstelle.mobile.android.skeleton.util.DateConverter;
import org.dieschnittstelle.mobile.android.skeleton.viewmodel.TaskDetailViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class TaskDetailViewActivity extends AppCompatActivity {
    private static final int READ_CONTACTS_REQUEST_CODE = 1111;
    protected static final String TASK_DETAIL_VIEW_KEY = "taskDetailViewObject";
    protected static final int RESULT_DELETE_OK = 99;
    private Task task;
    private TaskDetailViewModel viewModel;
    private Button pickDateBtn;
    private Button pickTimeBtn;
    private ArrayAdapter<String> selectedContactsAdapter;

    private final ActivityResultLauncher<Intent> mapViewLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), activityResult -> {
        if (activityResult.getResultCode() == TaskDetailViewMapActivity.RESULT_OK && activityResult.getData() != null) {
            Task.Location selectedLocation = (Task.Location) activityResult.getData().getSerializableExtra(TaskDetailViewMapActivity.LOCATION_VIEW_KEY);
            if (selectedLocation != null && selectedLocation.getName() != null && selectedLocation.getLatlng() != null) {
                Button locationBtn = findViewById(R.id.btnPickLocation);
                locationBtn.setText(selectedLocation.getName());
                task.setLocation(selectedLocation);
            }
        }
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TaskDetailViewModel.class);
        if (viewModel.getTask() == null) {
            task = (Task) getIntent().getSerializableExtra(TASK_DETAIL_VIEW_KEY);
            if (task == null) {
                task = new Task();
            }
            viewModel.setTask(task);
        }

        ActivityTaskDetailViewBinding taskDetailViewBinding = DataBindingUtil.setContentView(
                this,
                R.layout.activity_task_detail_view
        );
        taskDetailViewBinding.setTaskDetailViewModel(this.viewModel);
        taskDetailViewBinding.setLifecycleOwner(this);

        pickDateBtn = findViewById(R.id.btnPickDueDate);
        setDueDate();

        pickTimeBtn = findViewById(R.id.btnPickTime);
        setTimeLimit();

        Button pickLocationBtn = findViewById(R.id.btnPickLocation);
        pickLocationBtn.setOnClickListener(it -> showTaskLocationMapView());

        setPriorityDropDown();
        setContactDropDown();

        this.viewModel.isTaskOnSave().observe(this, onSave -> {
            if (onSave) {
                if (task.getName() == null || task.getName().isBlank()) {
                    showMessage("Cannot save mission. Please enter mission name");
                    return;
                }
                String date = (String) pickDateBtn.getText();
                String time = (String) pickTimeBtn.getText();
                if ((!date.isBlank() && time.isBlank()) || (date.isBlank() && !time.isBlank())) {
                    showMessage("Cannot save mission. Please enter both mission date and time");
                    return;
                }
                // DateFormat String 01.01.2025 01:00
                long expiryLong = DateConverter.fromDateString(date + " " + time);
                task.setExpiry(expiryLong);
                Intent returnIntent = new Intent();
                returnIntent.putExtra(TASK_DETAIL_VIEW_KEY, task);
                this.setResult(TaskDetailViewActivity.RESULT_OK, returnIntent);
                this.finish();
            }
        });

        this.viewModel.isTaskOnDelete().observe(this, onDelete -> {
            if (onDelete) {
                deleteAlertDialog();
            }
        });

        this.viewModel.isContactOnDelete().observe(this, onDelete -> {
            if (onDelete) {
                selectedContactsAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == READ_CONTACTS_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            viewModel.setupAvailableContactList(getContentResolver());
        }
    }

    private void deleteAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(android.R.layout.select_dialog_item);
        builder.setMessage("Delete Mission \"" + task.getName() + "\"?");
        builder.setPositiveButton("Delete", (dialog, id) -> deleteIntent());
        builder.setNegativeButton("Cancel", (dialog, id) -> {
           // do nothing
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteIntent() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(TASK_DETAIL_VIEW_KEY, task);
        this.setResult(TaskDetailViewActivity.RESULT_DELETE_OK, returnIntent);
        this.finish();
    }

    private void setDueDate() {
        pickDateBtn.setOnClickListener(view -> {
            Calendar calendar;
            int currentYear, currentMonth, currentDay;
            DatePickerDialog datePickerDialog;

            if (task.getExpiry() == 0) {
                calendar = Calendar.getInstance();
                currentYear = calendar.get(Calendar.YEAR);
                currentMonth = calendar.get(Calendar.MONTH);
                currentDay = calendar.get(Calendar.DAY_OF_MONTH);
            } else {
                String dateStr = DateConverter.toDateString(task.getExpiry()).split(" ")[0];
                currentDay = Integer.parseInt(dateStr.split("\\.")[0]);
                // Month of Calendar.java starts from 0
                currentMonth = Integer.parseInt(dateStr.split("\\.")[1]) - 1;
                currentYear = Integer.parseInt(dateStr.split("\\.")[2]);
            }
            datePickerDialog = setDatePickerDialog(currentYear, currentMonth, currentDay);
            datePickerDialog.show();
        });
    }

    @NonNull
    private DatePickerDialog setDatePickerDialog(int currentYear, int currentMonth, int currentDay) {
        return new DatePickerDialog(
                this,
                (datePickerView, selectedYear, selectedMonth, selectedDay) -> {
                    // Month of Calendar.java starts from 0
                    String dueDateStr = to2Digits(selectedDay) + "." + to2Digits(selectedMonth + 1) + "." + selectedYear;
                    pickDateBtn.setText(dueDateStr);
                },
                currentYear,
                currentMonth,
                currentDay
        );
    }

    private void setTimeLimit() {
        pickTimeBtn.setOnClickListener(view -> {
            Calendar calendar;
            int currentHour, currentMinute;
            TimePickerDialog timePickerDialog;

            if(task.getExpiry() == 0) {
                calendar = Calendar.getInstance();
                currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                currentMinute = calendar.get(Calendar.MINUTE);
            } else {
                String timeStr = DateConverter.toDateString(task.getExpiry()).split(" ")[1];
                currentHour = Integer.parseInt(timeStr.split(":")[0]);
                currentMinute = Integer.parseInt(timeStr.split(":")[1]);
            }
            timePickerDialog = setTimePickerDialog(currentHour, currentMinute);
            timePickerDialog.show();
        });
    }

    private TimePickerDialog setTimePickerDialog(int currentHour, int currentMinute) {
        return new TimePickerDialog(
                this,
                (timePickerView, selectedHour, selectedMinute) -> {
                    // convert single digit time to double digit. e.g.: 9h 0m -> 09:00
                    String timeLimitStr = to2Digits(selectedHour) + ":" + to2Digits(selectedMinute);
                    pickTimeBtn.setText(timeLimitStr);
                },
                currentHour,
                currentMinute,
                true
        );
    }

    private String to2Digits(int number) {
        if (number < 10) {
            return "0" + number;
        }
        return String.valueOf(number);
    }

    private void showTaskLocationMapView() {
        Intent callLocationViewIntent = new Intent(this, TaskDetailViewMapActivity.class);
        callLocationViewIntent.putExtra(TaskDetailViewMapActivity.LOCATION_VIEW_KEY, task.getLocation());
        mapViewLauncher.launch(callLocationViewIntent);
    }

    private void setPriorityDropDown() {
        final Spinner taskPrioritySpinner = findViewById(R.id.dropdownPriority);
        Task.Priority currentPriority = task.getPriority();
        List<String> priorities = new ArrayList<>(Arrays.asList(Task.Priority.NONE.name(), Task.Priority.LOW.name(), Task.Priority.NORMAL.name(), Task.Priority.HIGH.name(), Task.Priority.CRITICAL.name()));
        if (!currentPriority.equals(Task.Priority.NONE)) {
            priorities.add(0, currentPriority.name());
            priorities = priorities.stream().distinct().collect(Collectors.toList());
        }
        ArrayAdapter<String> dropDownPriorityAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item, priorities);
        dropDownPriorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        taskPrioritySpinner.setAdapter(dropDownPriorityAdapter);

        taskPrioritySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                String selectedPriority = taskPrioritySpinner.getSelectedItem().toString();
                task.setPriority(Task.Priority.valueOf(selectedPriority));
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void setContactDropDown() {
        getContactListFromContactApp();
        selectedContactsAdapter = new ContactListAdapter(this, R.layout.contact_item_view, viewModel.getTask().getContacts());
        final ListView selectedContacts = findViewById(R.id.selectedContacts);
        selectedContacts.setAdapter(selectedContactsAdapter);

        final Spinner contactsSpinner = findViewById(R.id.contactsDropdown);
        List<String> availableContactNames = new ArrayList<>(List.of("Select contact"));
        availableContactNames.addAll(
                viewModel.getAvailableContacts().stream()
                        .map(TaskDetailViewModel.Contact::getName)
                        .collect(Collectors.toList())
        );

        ArrayAdapter<String> availableContactAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, availableContactNames);
        contactsSpinner.setAdapter(availableContactAdapter);
        contactsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { return; }
                String selectedContactName = contactsSpinner.getSelectedItem().toString();

                if (viewModel.getTask().getContacts().stream().noneMatch(contact -> Objects.equals(contact, selectedContactName))) {
                    TaskDetailViewModel.Contact selectedContact = viewModel.getAvailableContacts().stream()
                            .filter(contact -> contact.getName().equals(selectedContactName))
                            .findFirst().orElse(null);

                    if (selectedContact == null) { return; }

                    viewModel.getTask().getContacts().add(selectedContact.getId());
                }

                contactsSpinner.setSelection(0, false);
                selectedContactsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void getContactListFromContactApp() {
        if (getApplicationContext().checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, READ_CONTACTS_REQUEST_CODE);
        } else {
            viewModel.setupAvailableContactList(getContentResolver());
        }
    }

    private void openSMSApp(String contactId, String taskName, String taskDescription) {
        TaskDetailViewModel.Contact recipient = viewModel.getAvailableContacts().stream()
                .filter(contact -> contact.getId().equals(contactId))
                .findAny()
                .orElse(null);

        if (recipient == null) {
            showMessage("Cannot find phone number of non existing recipient");
            return;
        }

        if (recipient.getPhoneNumbers() == null || recipient.getPhoneNumbers().isEmpty()) {
            showMessage("Cannot find phone number of " + recipient.getName());
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + recipient.getPhoneNumbers().get(0)));
        String message = "Mission Name: " + taskName + " Mission Description: " + taskDescription;
        intent.putExtra("sms_body", message);
        startActivity(intent);
    }

    private void openEMailApp(String contactId, String taskName, String taskDescription) {
        TaskDetailViewModel.Contact recipient = viewModel.getAvailableContacts().stream()
                .filter(contact -> contact.getId().equals(contactId))
                .findAny()
                .orElse(null);

        if (recipient == null) {
            showMessage("Cannot find email address of non existing recipient");
            return;
        }

        if (recipient.getEmails() == null || recipient.getEmails().isEmpty()) {
            showMessage("Cannot find email address of " + recipient.getName());
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient.getEmails().get(0)});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Mission Name: " + taskName);
        intent.putExtra(Intent.EXTRA_TEXT, " Mission Description: " + taskDescription);
        startActivity(intent);
    }

    private void showMessage(String message) {
        Snackbar.make(findViewById(R.id.taskDetailViewActivity), message, Snackbar.LENGTH_SHORT).show();
    }

    private class ContactListAdapter extends ArrayAdapter<String> {
        public ContactListAdapter(Context owner, int resourceId, List<String> contactList) {
            super(owner, resourceId, contactList);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View recyclableContactView, @NonNull ViewGroup parent) {
            View contactView;
            ContactItemViewBinding contactItemViewBinding;
            String selectedContactId = getItem(position);

            // recyclableContactView do not exist, then create one
            if (recyclableContactView == null) {
                contactItemViewBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.contact_item_view, null, false);
                contactView = contactItemViewBinding.getRoot();
                contactView.setTag(contactItemViewBinding);
                contactItemViewBinding.setTaskDetailViewModel(viewModel);

            // recyclableContactView exist, then reuse and access it with binding object
            } else {
                contactView = recyclableContactView;
                contactItemViewBinding = (ContactItemViewBinding) contactView.getTag();
            }

            if (selectedContactId == null || selectedContactId.isBlank()) {
                return contactView;
            }
            String contactName = viewModel.getAvailableContacts().stream()
                    .filter(contact -> contact.getId().equals(selectedContactId))
                    .map(TaskDetailViewModel.Contact::getName)
                    .findFirst().orElse(null);

            if (contactName == null || contactName.isBlank()) { return contactView; }

            contactItemViewBinding.setContact(contactName);

            ImageButton smsButton = contactView.findViewById(R.id.contactSmsButton);
            smsButton.setOnClickListener(it -> openSMSApp(selectedContactId, task.getName(), task.getDescription()));

            ImageButton mailButton = contactView.findViewById(R.id.contactMailButton);
            mailButton.setOnClickListener(it -> openEMailApp(selectedContactId, task.getName(), task.getDescription()));

            return contactView;
        }
    }
}