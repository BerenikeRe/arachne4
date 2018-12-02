package de.uni_koeln.arachne.export;

import de.uni_koeln.arachne.service.MailService;
import de.uni_koeln.arachne.service.Transl8Service;
import de.uni_koeln.arachne.service.UserRightsService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;

/**
 * @author Paf
 */

@Service
public class DataExportStack {

    @Autowired
    private transient UserRightsService userRightsService;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private transient MailService mailService;

    @Autowired
    private DataExportFileManager dataExportFileManager;

    @Autowired
    public transient Transl8Service transl8Service;

    private static final Logger LOGGER = LoggerFactory.getLogger("DataExportLogger");

    private Stack<DataExportTask> stack = new Stack<DataExportTask>();
    private ArrayList<DataExportTask> running = new ArrayList<DataExportTask>();
    private HashMap<String, DataExportTask> finished = new HashMap<String, DataExportTask>();

    @Value("${dataExportMaxStackSize:10}")
    private Integer dataExportMaxStackSize;

    @Value("${dataExportMaxThreads:4}")
    private Integer dataExportMaxThreads;

    @Value("${dataExportMaxTaskLifeTime:86400000}")
    private Integer dataExportMaxTaskLifeTime;

    public DataExportTask newTask(AbstractDataExportConverter converter, DataExportConversionObject conversionObject) {

        DataExportTask task = new DataExportTask(converter, conversionObject);
        task.setOwner(userRightsService.getCurrentUser());
        task.setRequestUrl(getRequestUrl());
        task.setBackendUrl(getBackendUrl());
        task.setUserRightsService(userRightsService);
        task.setLanguage(getRequestLanguage());

        return task;
    }

    public void push(DataExportTask task) {

        if (!userRightsService.isSignedInUser()) {
            LOGGER.info("Not logged in");
            throw new DataExportException("to_huge_and_not_logged_in", HttpStatus.UNAUTHORIZED);
        }

        LOGGER.info("Push task " + task.uuid.toString());
        LOGGER.info(stack.size() + " tasks in stack");

        if (stack.size() >= dataExportMaxStackSize) {
            throw new DataExportException("stack_full", HttpStatus.SERVICE_UNAVAILABLE);
        }

        if(running.size() >= dataExportMaxThreads) {
            stack.add(task);
            LOGGER.info("added task " + task.uuid.toString() + " to stack (" + stack.size() + ")");
        } else {
            runTask(task);
        }

    }

    public void runTask(DataExportTask task) {
        LOGGER.info("Run task: " + task.uuid.toString());
        task.startTimer();
        running.add(task);
        startThread(task);
    }

    public void nextTask() {
        LOGGER.info("Next task");
        try {
            runTask(stack.pop());
        } catch (EmptyStackException exception) {
            LOGGER.info("All tasks finished");

        }

    }

    public void removeFinishedTask(DataExportTask task) {
        finished.remove(task.uuid.toString());
    }

    private void startThread(DataExportTask task) {
        final DataExportThread dataExportThread = new DataExportThread(task, getRequest());
        taskExecutor.execute(dataExportThread);
        dataExportThread.setFileManager(dataExportFileManager);
        dataExportThread.registerListener(this);
    }

    public void taskIsFinishedListener(DataExportTask task) {
        task.stopTimer();
        running.remove(task);
        finished.put(task.uuid.toString(), task);


        if (!task.error) {
            String subject = "Arachne Data Export";
            String text = "%USER% | %URL% | %NAME%";
            final String user = (!task.getOwner().getLastname().equals("") && (task.getOwner().getLastname() != null))
                    ? task.getOwner().getFirstname() + " " + task.getOwner().getLastname()
                    : task.getOwner().getUsername();
            final String url = dataExportFileManager.getFileUrl(task);
            final String mail = task.getOwner().getEmail();
            try {
                subject = transl8Service.transl8("data_export_ready", task.getLanguage());
                text = transl8Service.transl8("data_export_success_mail", task.getLanguage());
            } catch (Transl8Service.Transl8Exception e) {
                // tranls8 is not available.. normal, we don't panic...
            }
            text = text
                    .replace("%URL%", url)
                    .replace("%USER%", user)
                    .replace("%NAME%", task.getConversionName());

            mailService.sendMailHtml(mail, null, subject, text);
        }

        LOGGER.info("Finished task:" + task.uuid.toString() + " " + running.size() + " tasks in stack");
        nextTask();
    }

    public DataExportTask getFinishedTaskById(String taskId) {
        if (!finished.containsKey(taskId)) {
            throw new DataExportException("task_not_found", HttpStatus.NOT_FOUND);
        }
        return finished.get(taskId);
    }

    public Integer getTasksRunning() {
        return stack.size();
    }

    public JSONObject getStatus() {
        final JSONObject status = new JSONObject();
        status.put("max_stack_size", dataExportMaxStackSize);
        status.put("max_threads", dataExportMaxThreads);
        status.put("tasks_running", running.size());
        status.put("tasks_enqueued", stack.size());
        final JSONObject taskList = new JSONObject();
        for (DataExportTask task: stack) {
            final JSONObject info = task.getInfoAsJSON();
            info.put("status", "enqueued");
            taskList.put(task.uuid.toString(), info);
        }
        for (DataExportTask task: running) {
            final JSONObject info = task.getInfoAsJSON();
            info.put("status", "running");
            taskList.put(task.uuid.toString(), info);
        }
        for (HashMap.Entry<String, DataExportTask> taskItem: finished.entrySet()) {
            final JSONObject info = taskItem.getValue().getInfoAsJSON();
            info.put("status", taskItem.getValue().error ? "error" : "finished");
            taskList.put(taskItem.getValue().uuid.toString(), info);
        }
        status.put("tasks", taskList);
        return status;
    }

    public ArrayList<DataExportTask> getOutdatedTasks() {
        final ArrayList<DataExportTask> outdatedTasks = new ArrayList<DataExportTask>();
        for (HashMap.Entry<String, DataExportTask> taskItem: finished.entrySet()) {
            if (taskItem.getValue().getAge() > dataExportMaxTaskLifeTime) {
                outdatedTasks.add(taskItem.getValue());
            }
        }
        return outdatedTasks;
    }


    private String getRequestLanguage() {
        final String langParameter = getRequest().getParameter("lang");
        final String langHeader = getRequest().getHeader("Accept-Language");
        return (langParameter == null) ? ((langHeader == null) ? "de" : langHeader) : langParameter;
    }

    private String getRequestUrl() {
        return getRequest().getRequestURL().toString() + "?" + getRequest().getQueryString();
    }

    private String getBackendUrl() {
        final String url = getRequest().getRequestURL().toString();
        return url.substring(0, url.lastIndexOf('/'));
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return sra.getRequest();
    }

}