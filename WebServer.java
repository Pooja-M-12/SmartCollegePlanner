import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class WebServer {

    static List<Task> tasks = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(9090), 0);

        server.createContext("/", new HomeHandler());
        server.createContext("/add", new AddTaskHandler());
        server.createContext("/tasks", new TaskListHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Server started at http://localhost:9090");
    }

    // ---------- TASK CLASS ----------
    static class Task {
        String name;
        String difficulty;
        LocalDate deadline;
        int priorityScore;

        static final int MAX_DAYS = 30;

        Task(String name, String difficulty, String deadlineStr) {
            this.name = name;
            this.difficulty = difficulty;
            this.deadline = LocalDate.parse(deadlineStr);
            this.priorityScore = calculatePriority();
        }

        private int calculatePriority() {
            int diffValue = 1;
            switch(difficulty.toLowerCase()) {
                case "medium": diffValue = 2; break;
                case "hard": diffValue = 3; break;
            }
            long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), deadline);
            if(daysUntil < 0) daysUntil = 0;
            return diffValue * 10 + (MAX_DAYS - (int) daysUntil);
        }
    }

    // ---------- HOME PAGE ----------
    static class HomeHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String html = "<!DOCTYPE html><html><head>"
                    + "<title>Smart College Planner</title>"
                    + "<style>"
                    + "body { font-family: Arial; background: #eef2f7; padding: 40px; }"
                    + ".container { background:white; width:400px; margin:auto; padding:25px; border-radius:8px; box-shadow:0 0 10px rgba(0,0,0,0.1);}"
                    + "h2 { text-align:center; color:#2c3e50; }"
                    + "label { font-weight:bold; display:block; margin-top:15px; }"
                    + "input, select { width:100%; padding:8px; margin-top:5px; border-radius:4px; border:1px solid #ccc; }"
                    + "button { margin-top:20px; width:100%; padding:10px; background:#3498db; color:white; border:none; border-radius:5px; font-size:16px; cursor:pointer;}"
                    + "button:hover { background:#2980b9; }"
                    + "a { text-decoration: none; display:block; text-align:center; margin-top:10px; color:#3498db; }"
                    + "</style></head><body>"
                    + "<div class='container'>"
                    + "<h2>Smart College Planner</h2>"
                    + "<form action='/add' method='get'>"
                    + "<label>Task</label><input type='text' name='task' required>"
                    + "<label>Difficulty</label><select name='difficulty'>"
                    + "<option>Easy</option><option>Medium</option><option>Hard</option>"
                    + "</select>"
                    + "<label>Deadline</label><input type='date' name='deadline' required>"
                    + "<button type='submit'>Add Task</button>"
                    + "</form>"
                    + "<a href='/tasks'>View All Tasks</a>"
                    + "</div></body></html>";

            ex.sendResponseHeaders(200, html.getBytes().length);
            OutputStream os = ex.getResponseBody();
            os.write(html.getBytes());
            os.close();
        }
    }

    // ---------- ADD TASK ----------
    static class AddTaskHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String query = ex.getRequestURI().getQuery();
            String task = "", diff = "", deadline = "";

            if(query != null){
                String[] params = query.split("&");
                for(String p : params){
                    String[] kv = p.split("=");
                    String key = kv[0];
                    String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    if(key.equals("task")) task=value;
                    if(key.equals("difficulty")) diff=value;
                    if(key.equals("deadline")) deadline=value;
                }
            }

            tasks.add(new Task(task, diff, deadline));

            String response = "<!DOCTYPE html><html><head><title>Task Added</title>"
                    + "<style>body{font-family:Arial;text-align:center;padding:50px;} a{color:#3498db;display:block;margin-top:20px;}</style>"
                    + "</head><body>"
                    + "<h2>Task Added ✅</h2>"
                    + "<p><b>"+task+"</b> | "+diff+" | "+deadline+"</p>"
                    + "<a href='/'>Add Another Task</a>"
                    + "<a href='/tasks'>View All Tasks</a>"
                    + "</body></html>";

            ex.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = ex.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // ---------- TASK LIST PAGE ----------
    static class TaskListHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {

            tasks.sort(Comparator.comparingInt((Task t) -> t.priorityScore).reversed());

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><title>All Tasks</title>")
                .append("<style>")
                .append("body{font-family:Arial;background:#eef2f7;padding:40px;} .task-card{padding:15px;margin:10px;border-radius:8px;color:white;}")
                .append(".red{background:#e74c3c;} .orange{background:#f39c12;} .green{background:#2ecc71;}")
                .append("a{display:block;margin:20px auto;text-align:center;text-decoration:none;color:#3498db;}")
                .append("</style></head><body>")
                .append("<h2 style='text-align:center'>All Tasks 📋 (Sorted by Priority)</h2>");

            for(Task t: tasks){
                String colorClass;
                if(t.priorityScore >= 40) colorClass = "red";
                else if(t.priorityScore >= 25) colorClass = "orange";
                else colorClass = "green";

                html.append("<div class='task-card ").append(colorClass).append("'>")
                    .append("<b>").append(t.name).append("</b><br>")
                    .append("Difficulty: ").append(t.difficulty).append("<br>")
                    .append("Deadline: ").append(t.deadline).append("<br>")
                    .append("Priority: ").append(t.priorityScore)
                    .append("</div>");
            }

            html.append("<a href='/'>Add New Task</a>");
            html.append("</body></html>");

            String response = html.toString();
            ex.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = ex.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
